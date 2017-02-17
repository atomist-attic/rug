package com.atomist.rug.runtime.plans

import com.atomist.rug.spi.Handlers.Instruction.Respond
import com.atomist.rug.spi.Handlers.Status.{Failure, Success}
import com.atomist.rug.spi.Handlers._

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
                      nestedPlanRunner: Option[PlanRunner] = None) extends PlanRunner {

  override def run(plan: Plan, callbackInput: AnyRef): Future[PlanResult] = {
    val messageLog: Seq[MessageDeliveryError] = plan.messages.flatMap { message =>
      Try(messageDeliverer.deliver(message, callbackInput)) match {
        case ScalaFailure(e) =>  Some(MessageDeliveryError(message, e))
        case ScalaSuccess(_) => None
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
      case ScalaFailure(t) =>
        t.printStackTrace()
        Seq(InstructionError(respondable.instruction, t))
      case ScalaSuccess(response) =>
        val callbacks: Seq[Callback] = response match {
          case Response(Success, _, _, Some(plan: Plan)) =>
            Seq(Some(plan), respondable.onSuccess).flatten
          case Response(Success, _, _, _) => Seq(respondable.onSuccess).flatten
          case Response(Failure, _, _, _) => Seq(respondable.onFailure).flatten
        }
        val callbackResultOption: Seq[PlanLogEvent] = callbacks.flatMap { callback =>
          Try(handleCallback(callback, response.body)) match {
            case ScalaFailure(error) =>
              Some(CallbackError(callback, error))
            case ScalaSuccess(nestedPlanExecutionOption) =>
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
