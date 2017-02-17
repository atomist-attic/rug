package com.atomist.rug.runtime.execution

import com.atomist.rug.spi.Handlers.Instruction.Respond
import com.atomist.rug.spi.Handlers.Status.{Failure, Success}
import com.atomist.rug.spi.Handlers._

import scala.util.{Try, Failure => ScalaFailure, Success => ScalaSuccess}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.atomist.rug.spi.JavaHandlersConverter._

class LocalPlanExecutor(messageDeliverer: MessageDeliverer,
                        instructionExecutor: InstructionExecutor,
                        nestedPlanExecutor: Option[PlanExecutor] = None) extends PlanExecutor {

  override def execute(plan: Plan, callbackInput: AnyRef): Future[PlanResult] = {
    val messageLog: Seq[MessageDeliveryError] = plan.messages.flatMap { message =>
      Try(messageDeliverer.deliver(toJavaMessage(message), callbackInput)) match {
        case ScalaFailure(e) =>  Some(MessageDeliveryError(message, e))
        case ScalaSuccess(_) => None
      }
    }
    val instructionResponseFutures: Seq[Future[Iterable[PlanLogEvent]]] = plan.instructions.map { respondable =>
      Future {
        Try { instructionExecutor.execute(toJavaInstruction(respondable.instruction), callbackInput) } match {
          case ScalaFailure(t) =>
            Seq(InstructionError(respondable.instruction, t))
          case ScalaSuccess(r) =>
            val response = toScalaResponse(r)
            val callbackOption = response match {
              case Response(Success, _, _, _) => respondable.onSuccess
              case Response(Failure, _, _, _) => respondable.onFailure
            }
            val callbackResultOption: Option[PlanLogEvent] = callbackOption.flatMap { callback =>
              Try(handleCallback(callback, response.body)) match {
                case ScalaFailure(error) =>
                  Some(CallbackError(callback, error))
                case ScalaSuccess(nestedPlanExecutionOption) =>
                  nestedPlanExecutionOption
              }
            }
            Seq(
              Some(InstructionResponse(respondable.instruction, response)),
              callbackResultOption
            ).flatten
        }
      }
    }
    val futureInstructionLog: Future[Seq[PlanLogEvent]] = Future.fold(instructionResponseFutures)(Seq[PlanLogEvent]())(_ ++ _)
    futureInstructionLog.map(instructionLogEvents => PlanResult(messageLog ++ instructionLogEvents))
  }

  private def handleCallback(callback: Callback, instructionResult: Option[AnyRef]): Option[PlanLogEvent] = callback match {
    case m: Message =>
      messageDeliverer.deliver(toJavaMessage(m), instructionResult.orNull)
      None
    case r: Respond =>
      instructionExecutor.execute(toJavaInstruction(r), instructionResult.orNull)
      None
    case p: Plan =>
      val planExecutor = nestedPlanExecutor.getOrElse(new LocalPlanExecutor(messageDeliverer, instructionExecutor, nestedPlanExecutor))
      val planExecution = planExecutor.execute(p, instructionResult.orNull)
      Some(NestedPlanExecution(p, planExecution))
  }

}
