package com.atomist.rug.runtime.plans

import com.atomist.rug.spi.Handlers.Instruction.Respond
import com.atomist.rug.spi.Handlers.Status.{Failure, Success}
import com.atomist.rug.spi.Handlers._
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Try, Failure => ScalaFailure, Success => ScalaSuccess}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

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

  override def run(plan: Plan, callbackInput: AnyRef): Future[PlanResult] = {
    val messageLog: Seq[MessageDeliveryError] = plan.messages.flatMap { message =>
      Try(messageDeliverer.deliver(message, callbackInput)) match {
        case ScalaFailure(error) =>
          val msg = s"Failed to deliver message: ${message.body} - ${error.getMessage}"
          logger.error(msg)
          Some(MessageDeliveryError(message, error))
        case ScalaSuccess(_) =>
          val msg = s"Delivered message: ${message.body}"
          logger.info(msg)
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

  private def handleInstruction(respondable: Respondable, callbackInput: AnyRef): Seq[PlanLogEvent] = {
    Try { instructionRunner.run(respondable.instruction, callbackInput) } match {
      case ScalaFailure(error) =>
        val msg = s"Failed to run instruction: ${respondable.instruction} - ${error.getMessage}"
        logger.error(msg)
        Seq(InstructionError(respondable.instruction, error))

      case ScalaSuccess(response) =>
        val msg = s"Ran instruction: ${respondable.instruction} and got response: $response"
        logger.info(msg)
        val callbacks: Seq[Callback] = response match {
          case Response(Success, _, _, Some(plan: Plan)) =>
            Seq(Some(plan), respondable.onSuccess).flatten
          case Response(Success, _, _, _) => Seq(respondable.onSuccess).flatten
          case Response(Failure, _, _, _) => Seq(respondable.onFailure).flatten
        }
        val callbackResultOption: Seq[PlanLogEvent] = callbacks.flatMap { callback =>
          Try(handleCallback(callback, response.body)) match {
            case ScalaFailure(error) =>
              val msg = s"Failed to run $callback after ${respondable.instruction} - ${error.getMessage}"
              logger.error(msg)
              Some(CallbackError(callback, error))
            case ScalaSuccess(nestedPlanExecutionOption) =>
              val msg = s"Ran $callback after ${respondable.instruction}"
              logger.info(msg)
              nestedPlanExecutionOption
          }
        }
        callbackResultOption :+ InstructionResponse(respondable.instruction, response)
    }
  }

  private def handleCallback(callback: Callback, instructionResult: Option[AnyRef]): Seq[PlanLogEvent] = callback match {
    case m: Message =>
      messageDeliverer.deliver(m, instructionResult.orNull)
      Nil
    case r: Respond =>
      handleInstruction(Respondable(r, None, None), instructionResult.orNull)
    case p: Plan =>
      val planResult = runNestedPlan(p, instructionResult)
      Seq(NestedPlanRun(p, planResult))
  }

  private def runNestedPlan(plan: Plan, input: Option[AnyRef] = None) = {
    val planRunner = nestedPlanRunner.getOrElse(new LocalPlanRunner(messageDeliverer, instructionRunner, nestedPlanRunner))
    planRunner.run(plan, input.orNull)
  }
}
