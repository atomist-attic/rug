package com.atomist.rug.runtime.plans

import com.atomist.rug.spi.Handlers.Instruction.Respond
import com.atomist.rug.spi.Handlers.Status.{Failure, Success}
import com.atomist.rug.spi.Handlers._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Try, Failure => ScalaFailure, Success => ScalaSuccess}
import scala.concurrent.{Await, Future}

/**
  * Runs Plans in this JVM - i.e. no work distribution.
  */
class LocalPlanRunner(messageDeliverer: MessageDeliverer,
                      instructionRunner: InstructionRunner,
                      nestedPlanRunner: Option[PlanRunner] = None,
                      loggerOption: Option[Logger] = None) extends PlanRunner {

  private val logger: Logger = loggerOption.getOrElse(LoggerFactory getLogger getClass.getName)

  override def run(plan: Plan, callbackInput: Option[Response]): Future[PlanResult] = {
    val allMessages = plan.lifecycle ++ plan.local
    val messageLog: Seq[MessageDeliveryError] = allMessages.flatMap { message =>
      if (plan.returningRug.nonEmpty) {
        Try(messageDeliverer.deliver(plan.returningRug.get, message, callbackInput)) match {
          case ScalaFailure(error) =>
            val msg = s"Failed to deliver message ${message.toDisplay} - ${error.getMessage}"
            logger.error(msg, error)
            Some(MessageDeliveryError(message, error))
          case ScalaSuccess(_) =>
            val msg = s"Delivered message ${message.toDisplay}"
            logger.debug(msg)
            None
        }
      } else {
        val msg = s"Failed to deliver message ${message.toDisplay} - current Rug not available for dependency resolution"
        logger.error(msg)
        None
      }
    }
    val instructionResponseFutures: Seq[Future[Iterable[PlanLogEvent]]] = plan.instructions.map { respondable =>
      val instruction = handleInstruction(plan, respondable, callbackInput)
      Future {
        instruction
      }
    }
    val futureInstructionLog: Future[Seq[PlanLogEvent]] = Future.fold(instructionResponseFutures)(Seq[PlanLogEvent]())(_ ++ _)
    futureInstructionLog.map(instructionLogEvents => PlanResult(messageLog ++ instructionLogEvents))
  }

  private def handleInstruction(currentPlan: Plan, plannable: Plannable, callbackInput: Option[Response]): Seq[PlanLogEvent] = {
    Try {
      instructionRunner.run(plannable.instruction, callbackInput)
    } match {
      case ScalaFailure(error) =>
        val msg = s"Failed to run ${plannable.toDisplay} - ${error.getMessage}"
        logger.error(msg, error)
        Seq(InstructionError(plannable.instruction, error))
      case ScalaSuccess(response) =>
        val msg = s"Ran ${plannable.toDisplay} and then ${response.toDisplay}"
        logger.debug(msg)
        val callbackOption: Option[Callback] = plannable match {
          case _: Nonrespondable =>
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
          Try(handleCallback(currentPlan, callback, Some(response))) match {
            case ScalaFailure(error) =>
              val msg = s"Failed to invoke ${callback.toDisplay} after ${plannable.toDisplay} - ${error.getMessage}"
              logger.error(msg, error)
              Seq(CallbackError(callback, error))
            case ScalaSuccess(nestedPlanExecutionOption) =>
              val msg = s"Invoked ${callback.toDisplay} after ${plannable.toDisplay}"
              logger.debug(msg)
              nestedPlanExecutionOption
          }
        }
        //ensure handled errors don't return failure https://github.com/atomist/rug/issues/531
        (response, callbackResults, callbackOption) match {
          case (Response(Failure, _, _, _), Some(logs), Some(callback)) if !PlanResultInterpreter.hasLogFailure(logs) && callback.isInstanceOf[Respond] =>
            val handled = Response(Status.Handled, response.msg, response.code, response.body)
            Seq(callbackResults, Some(Seq(InstructionResult(plannable.instruction, handled)))).flatten.flatten
          case _ =>
            Seq(callbackResults, Some(Seq(InstructionResult(plannable.instruction, response)))).flatten.flatten
        }
    }
  }

  private def handleCallback(currentPlan: Plan, callback: Callback, instructionResult: Option[Response]): Seq[PlanLogEvent] = callback match {
    case m: Message =>
      if (currentPlan.returningRug.nonEmpty) {
        Try(messageDeliverer.deliver(currentPlan.returningRug.get, m, instructionResult)) match {
          case ScalaFailure(error) =>
            val msg = s"Failed to deliver message ${m.toDisplay} - ${error.getMessage}"
            logger.error(msg, error)
          case ScalaSuccess(_) =>
            val msg = s"Delivered message ${m.toDisplay}"
            logger.debug(msg)
        }
      } else {
        val msg = s"Failed to deliver message ${m.toDisplay} - current Rug not available for dependency resolution"
        logger.error(msg)
      }
      Nil
    case r: Respond =>
      handleInstruction(currentPlan, Nonrespondable(r), instructionResult)
    case p: Plan =>
      val planResult = runNestedPlan(p, instructionResult)
      //wait for nested plans because we need onSuccess/onError handlers to fire for `command` instructions
      Await.result(planResult, Duration("10min"))
      Seq(NestedPlanRun(p, planResult))
  }

  private def runNestedPlan(plan: Plan, input: Option[Response] = None) = {
    val planRunner = nestedPlanRunner.getOrElse(new LocalPlanRunner(messageDeliverer, instructionRunner, nestedPlanRunner, loggerOption))
    planRunner.run(plan, input)
  }
}
