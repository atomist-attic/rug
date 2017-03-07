package com.atomist.rug.runtime.plans

import com.atomist.rug.spi.Handlers.Instruction.Respond
import com.atomist.rug.spi.Handlers.Status.{Failure, Success}
import com.atomist.rug.spi.Handlers._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Try, Failure => ScalaFailure, Success => ScalaSuccess}

/**
  * Runs Plans in this JVM - i.e. no work distribution.
  * @param messageDeliverer
  * @param instructionRunner
  * @param nestedPlanRunner
  */
class LocalPlanRunner(messageDeliverer: MessageDeliverer,
                      instructionRunner: InstructionRunner,
                      nestedPlanRunner: Option[PlanRunner] = None,
                      loggerOption: Option[Logger] = None) extends PlanRunner {

  private val logger: Logger = loggerOption.getOrElse(LoggerFactory getLogger getClass.getName)

  override def run(plan: Plan, callbackInput: Option[Response]): Future[PlanResult] = {
    val messageLog: Seq[MessageDeliveryError] = plan.messages.flatMap { message =>
      Try(messageDeliverer.deliver(message, callbackInput)) match {
        case ScalaFailure(error) =>
          val msg = s"Failed to deliver message: ${message.body} - ${error.getMessage}"
          logger.error(msg, error)
          Some(MessageDeliveryError(message, error))
        case ScalaSuccess(_) =>
          val msg = s"Delivered message: ${message.body}"
          logger.debug(msg)
          None
      }
    }
    val instructionResponseFutures: Seq[Future[Iterable[PlanLogEvent]]] = plan.instructions.map { respondable =>
      Future {
        handleInstruction(respondable, callbackInput)
      }
    }
    val futureInstructionLog: Future[Seq[PlanLogEvent]] = Future.fold(instructionResponseFutures)(Seq[PlanLogEvent]())(_ ++ _)
    futureInstructionLog.map(instructionLogEvents => PlanResult(messageLog ++ instructionLogEvents))
  }

  private def handleInstruction(plannable: Plannable, callbackInput: Option[Response]): Seq[PlanLogEvent] = {
    Try { instructionRunner.run(plannable.instruction, callbackInput) } match {
      case ScalaFailure(error) =>
        val msg = s"Failed to run instruction: ${plannable.instruction} - ${error.getMessage}"
        logger.error(msg, error)
        Seq(InstructionError(plannable.instruction, error))
      case ScalaSuccess(response) =>
        val msg = s"Ran instruction: ${plannable.instruction} and got response: $response"
        logger.debug(msg)
        val callbackOption: Option[Callback] = plannable match {
          case nr: Nonrespondable =>
            response match {
              case Response(Success, _, _, Some(plan: Plan)) => Some(plan)
              case _ => None
            }
          case respondable: Respondable =>
            response match {
              case Response(Success, _, _, _) => respondable.onSuccess
              case Response(Failure, _, _, _) => respondable.onFailure
            }

        }
        val callbackResults: Option[Seq[PlanLogEvent]] = callbackOption.map { callback =>
          Try(handleCallback(callback, Some(response))) match {
            case ScalaFailure(error) =>
              val msg = s"Failed to run $callback after ${plannable.instruction} - ${error.getMessage}"
              logger.error(msg, error)
              Seq(CallbackError(callback, error))
            case ScalaSuccess(nestedPlanExecutionOption) =>
              val msg = s"Ran $callback after ${plannable.instruction}"
              logger.debug(msg)
              nestedPlanExecutionOption
          }
        }
        Seq(callbackResults, Some(Seq(InstructionResult(plannable.instruction, response)))).flatten.flatten
    }
  }

  private def handleCallback(callback: Callback, instructionResult: Option[Response]): Seq[PlanLogEvent] = callback match {
    case m: Message =>
      messageDeliverer.deliver(m, instructionResult)
      Nil
    case r: Respond =>
      handleInstruction(Nonrespondable(r), instructionResult)
    case p: Plan =>
      val planResult = runNestedPlan(p, instructionResult)
      Seq(NestedPlanRun(p, planResult))
  }

  private def runNestedPlan(plan: Plan, input: Option[Response] = None) = {
    val planRunner = nestedPlanRunner.getOrElse(new LocalPlanRunner(messageDeliverer, instructionRunner, nestedPlanRunner, loggerOption))
    planRunner.run(plan, input)
  }
}
