package com.atomist.rug.spi

import com.atomist.graph.GraphNode
import com.atomist.param.ParameterValue
import com.atomist.rug.spi.Handlers.Instruction.{Detail, NonrespondableInstruction, RespondableInstruction}
import com.atomist.tree.TreeNode

import scala.concurrent.Future

/**
  * Beans that map to the @atomist/rug/operations/operations/Handlers
  */
object Handlers {

  /**
    * A plan in response to this event
    * @param lifecycle messages in the plan
    * @param local messages in the plan
    * @param instructions instructions in the plan
    * @param nativeObject native object (such as a Nashorn ScriptObjectMirror)
    *                     if one is available
    */
  case class Plan(lifecycle: Seq[LifecycleMessage],
                  local: Seq[LocallyRenderedMessage],
                  instructions: Seq[Plannable],
                  nativeObject: Option[AnyRef] = None) extends Callback {

    def messages: Seq[Message] = local ++ lifecycle

    override def toDisplay: String = {
      s"Plan[${(messages.map(_.toDisplay) ++ instructions.map(_.toDisplay)).mkString(", ")}]"
    }
  }

  trait Message extends Callback {}

  case class LifecycleMessage(
                               node: GraphNode,
                               actions: Seq[Presentable])
    extends Callback
      with Message {

    override def toDisplay: String = {
      s"tags: ${node.nodeTags.mkString}, actions: ${actions.mkString}"
    }
  }

  /**
    * Rendering/correlation is done in the handler
    * Directed & Response messages become these
    * @param body
    * @param channelNames
    * @param contentType
    * @param usernames
    */
  case class LocallyRenderedMessage(
                              body: String,
                              contentType: String,
                              channelNames: Seq[String],
                              usernames: Seq[String])
    extends Callback
      with Message {

    override def toDisplay: String = {
      s"channels: ${channelNames.mkString}, usernames: ${usernames.mkString}, type: $contentType, body: $body"
    }
  }

  sealed trait Callback {
    def toDisplay: String
  }

  sealed trait Plannable {
    def instruction: Instruction
    def toDisplay: String = instruction.toDisplay
  }

  case class Respondable(instruction: RespondableInstruction,
                         onSuccess: Option[Callback],
                         onFailure: Option[Callback]) extends Plannable

  case class Nonrespondable(instruction: NonrespondableInstruction) extends Plannable

  case class Presentable(instruction: Instruction, label: Option[String])

  //likely to change

  sealed trait Instruction {
    def detail: Detail

    def toDisplay: String = {
      val name = this.getClass.getName.split('$').last
      s"$name ${detail.name}(${detail.parameters.mkString(", ")})"
    }
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

    sealed trait RespondableInstruction extends Instruction
    sealed trait NonrespondableInstruction extends Instruction

    case class Generate(detail: Detail) extends RespondableInstruction

    case class Edit(detail: Detail) extends RespondableInstruction

    case class Review(detail: Detail) extends RespondableInstruction

    case class Execute(detail: Detail) extends RespondableInstruction

    case class Command(detail: Detail) extends NonrespondableInstruction

    case class Respond(detail: Detail) extends NonrespondableInstruction with Callback

  }

  case class MavenCoordinate(group: String,
                             artifact: String) {
  }

  case class Response(status: Status,
                      msg: Option[String] = None,
                      code: Option[Int] = None,
                      body: Option[AnyRef] = None) {
    def toDisplay: String = {
      val name = status.getClass.getName.split('$').last
      val value = (msg, code) match {
        case (Some(m), Some(c)) => s":$c:$m"
        case (Some(m), None) => s":$m"
        case (None, Some(c)) => s":${c.toString}"
        case _ => ""
      }
      s"$name$value"
    }
  }

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
