package com.atomist.rug.runtime.execution

import com.atomist.rug.spi.Handlers.Instruction.Respond
import com.atomist.rug.spi.Handlers.Status.{Failure, Success}
import com.atomist.rug.spi.Handlers._

import scala.util.{Try, Failure => ScalaFailure, Success => ScalaSuccess}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

import com.atomist.rug.spi.JavaHandlersConverter._

class LocalPlanExecutor(messageDeliverer: MessageDeliverer,
                        instructionExecutor: InstructionExecutor,
                        nestedPlanExecutor: Option[PlanExecutor] = None) extends PlanExecutor {

  private var instructionToReponseFutures: Map[Instruction, Future[Option[Response]]] = Map()
  private var messageDeliveryErrors: Map[Message, Throwable] = Map()
  private var instructionErrors: Map[Instruction, Throwable] = Map()
  private var callbackErrors: Map[Callback, Throwable] = Map()

  override def execute(plan: Plan, callbackInput: AnyRef): PlanResponse = {
    plan.messages.foreach { message =>
      Try(messageDeliverer.deliver(toJavaMessage(message), callbackInput)).recover {
        case e => messageDeliveryErrors += (message -> e)
      }
    }
    plan.instructions.foreach{ respondable =>
      val f = Future {
        Try { instructionExecutor.execute(toJavaInstruction(respondable.instruction), callbackInput) } match {
          case ScalaFailure(t) =>
            instructionErrors += (respondable.instruction -> t)
            None
          case ScalaSuccess(r) =>
            val response = toScalaResponse(r)
            val callbackOption = response match {
              case Response(Success, _, _, _) => respondable.onSuccess
              case Response(Failure, _, _, _) => respondable.onFailure
            }
            callbackOption.foreach { callback =>
              Try(handleCallback(callback, response.body)).recover {
                case e => callbackErrors += (callback -> e)
              }
            }
            Some(response)
        }
      }
      instructionToReponseFutures += (respondable.instruction -> f)
    }
    val futureResponses = Future.traverse(instructionToReponseFutures) { case (k, f) => f.map(k -> _) } map(_.toMap)
    val instructionToReponseTrys = Await.result(futureResponses, Duration.Inf)
    val instructionToReponses = instructionToReponseTrys.flatMap { case (k, t) => t.map(k -> _) }
    PlanResponse(instructionToReponses, messageDeliveryErrors, instructionErrors, callbackErrors)
  }

  private def handleCallback(callback: Callback, instructionResult: Option[AnyRef]): Unit = callback match {
    case m: Message => messageDeliverer.deliver(toJavaMessage(m), instructionResult.orNull)
    case r: Respond => instructionExecutor.execute(toJavaInstruction(r), instructionResult.orNull)
    case p: Plan => nestedPlanExecutor.getOrElse(this).execute(p, instructionResult.orNull)
  }

}
