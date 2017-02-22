package com.atomist.rug.runtime.plans

import com.atomist.rug.spi.Handlers.Instruction._
import com.atomist.tree.utils.TreeNodePrinter
import com.atomist.tree.utils.TreeNodePrinter.BabyTree

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Print out Plans etc
  */
object PlanUtils {

  import com.atomist.rug.spi.Handlers._

  def drawPlan(plan: Plan): String = {
    val tree: BabyTree = planToTree(plan)
    TreeNodePrinter.draw[BabyTree](_.children, _.print)(tree)
  }

  def drawEventLogs(name: String, events: Seq[PlanLogEvent]) =
    TreeNodePrinter.drawTree(awaitAndTreeLogEvents(name, events))

  private def instructionToString(i: Instruction) = {
    val op = i match {
      case Generate(detail) => "Generate"
      case Edit(detail) => "Edit"
      case Review(detail) => "Review"
      case Execute(detail) => "Execute"
      case Command(detail) => "Command"
      case Respond(detail) => "Respond"
    }
    s"${op} ${i.detail.name} ${i.detail.coordinates} ${i.detail.parameters}"
  }

  private def messageToTree(m: Message) = {
    val messageString = s"Message: ${m.body}${m.channelId.map(" to " + _).getOrElse("")}, like ${m.instructions}"
    BabyTree(messageString)
  }
  private def callbackToTree(name: String, c: Callback): BabyTree = BabyTree(name, Seq(c match {
    case p: Plan => planToTree(p)
    case m: Message => messageToTree(m)
    case r: Respond => BabyTree(instructionToString(r))
  }))

  private def planToTree(plan: Plan): BabyTree = {

    def respondableToTree(respondable: Respondable): BabyTree = {
      val i = instructionToString(respondable.instruction)

      val onSuccessChild = respondable.onSuccess.map(callbackToTree("onSuccess", _))
      val onFailureChild = respondable.onFailure.map(callbackToTree("onFailure", _))

      BabyTree(i, onSuccessChild.toSeq ++ onFailureChild)
    }

    BabyTree("Plan",
      plan.instructions.sortBy(_.toString).map(respondableToTree) ++
        plan.messages.sortBy(_.toString).map(messageToTree))
  }


  private def awaitAndTreeLogEvents(name: String, events: Seq[PlanLogEvent]): BabyTree = {
    def logEventToTree(one: PlanLogEvent): BabyTree = {
      def nullSafeMessage(x: Throwable) = s"Error: ${Option(x).map(_.getMessage).getOrElse("null exception")}"

      one match {
        case InstructionError(i, x) => BabyTree(nullSafeMessage(x), Seq(BabyTree(instructionToString(i))))
        case MessageDeliveryError(m, x) => BabyTree(nullSafeMessage(x), Seq(messageToTree(m)))
        case CallbackError(c, x) => BabyTree(nullSafeMessage(x), Seq(callbackToTree("failed callback", c)))
        case InstructionResponse(instruction, response) =>
          BabyTree(s"Received $response back from ${instructionToString(instruction)}")
        case NestedPlanRun(plan, planResult) =>
          Await.ready(planResult, 10.seconds)
          val result = planResult.value.get match {
            case Failure(exception) => BabyTree(s"Future failed with ${exception.getMessage}")
            case Success(value) => awaitAndTreeLogEvents(s"Future completed", value.log)
          }
          val planChild = planToTree(plan)
          BabyTree("Nested plan run", Seq(planChild, result))
      }
    }

    val children = events.map(logEventToTree)
    BabyTree(name, children.sortBy(_.print))
  }

}
