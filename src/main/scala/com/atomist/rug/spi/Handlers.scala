package com.atomist.rug.spi

import com.atomist.param.ParameterValue
import com.atomist.rug.spi.Handlers.Instruction.Detail

import scala.concurrent.Future

/**
  * Beans that map to the @atomist/rug/operations/operations/Handlers
  */
object Handlers {

  case class Plan(messages: Seq[Message], instructions: Seq[Respondable]) extends Callback

  case class Message(body: MessageBody,
                     instructions: Seq[Presentable],
                     channelId: Option[String]) extends Callback

  sealed trait Callback

  case class Respondable(instruction: Instruction,
                         onSuccess: Option[Callback],
                         onFailure: Option[Callback])

  case class Presentable(instruction: Instruction, label: Option[String])

  //likely to change

  sealed trait Instruction {
    def detail: Detail
  }

  object Instruction {

    //probably makes more sense to put project_name elsewhere
    case class Detail(name: String,
                      coordinates: Option[MavenCoordinate],
                      parameters: Seq[ParameterValue],
                      projectName: Option[String])

    def from(name: String, detail: Detail): Instruction = {
      name match {
        case "generate" => Generate(detail)
        case "edit" => Edit(detail)
        case "review" => Review(detail)
        case "execute" => Execute(detail)
        case "command" => Command(detail)
        case "respond" => Respond(detail)
        case _ => throw new IllegalArgumentException(s"Cannot derive Instruction from '$name'.")
      }
    }

    case class Generate(detail: Detail) extends Instruction

    case class Edit(detail: Detail) extends Instruction

    case class Review(detail: Detail) extends Instruction

    case class Execute(detail: Detail) extends Instruction

    case class Command(detail: Detail) extends Instruction

    case class Respond(detail: Detail) extends Instruction with Callback

  }

  case class MavenCoordinate(group: String,
                             artifact: String) {
  }

  sealed trait MessageBody

  case class JsonBody(json: String) extends MessageBody

  case class MessageText(text: String) extends MessageBody

  case class Response(status: Status,
                      msg: Option[String] = None,
                      code: Option[Int] = None,
                      body: Option[AnyRef] = None)

  case class PlanResult(log: Seq[PlanLogEvent])

  sealed trait PlanLogEvent

  sealed trait PlanLogError extends PlanLogEvent {
    def error: Throwable
  }

  case class InstructionResult(instruction: Instruction, response: Response) extends PlanLogEvent

  case class NestedPlanRun(plan: Plan, planResult: Future[PlanResult]) extends PlanLogEvent

  case class InstructionError(instruction: Instruction, error: Throwable) extends PlanLogError

  case class MessageDeliveryError(message: Message, error: Throwable) extends PlanLogError

  case class CallbackError(callback: Callback, error: Throwable) extends PlanLogError

  sealed trait Status

  object Status {

    case object Success extends Status

    case object Failure extends Status

    def from(name: String): Status = {
      name match {
        case "success" => Success
        case "failure" => Failure
        case _ => throw new IllegalArgumentException(s"Cannot derive Status from '$name'.")
      }
    }
  }

}
