package com.atomist.rug.test.gherkin.handler

import com.atomist.project.archive.Rugs
import com.atomist.rug.RugNotFoundException
import com.atomist.rug.runtime.CommandHandler
import com.atomist.rug.runtime.js.RugContext
import com.atomist.rug.spi.Handlers.{Message, Plan}
import com.atomist.rug.test.gherkin.{Definitions, ScenarioWorld}
import com.atomist.tree.TreeMaterializer

/**
  * Superclass for Handler worlds. Handles plan capture and exposing to JavaScript
  */
abstract class AbstractHandlerScenarioWorld(definitions: Definitions, rugs: Option[Rugs])
  extends ScenarioWorld(definitions, rugs) {

  private var planOption: Option[Plan] = None

  protected def createRugContext(tm: TreeMaterializer): RugContext =
    new FakeRugContext("team_id", typeRegistry, tm)

  /**
    * Return the editor with the given name or throw an exception
    */
  def commandHandler(name: String): CommandHandler = {
    rugs match {
      case Some(r) =>
        r.commandHandlers.find(e => e.name == name) match {
          case Some(e) => e
          case _ => throw new RugNotFoundException(
            s"CommandHandler with name '$name' can not be found in current context. Known CommandHandlers are [${r.commandHandlerNames.mkString(", ")}]")
        }
      case _ => throw new RugNotFoundException("No context provided")
    }
  }

  protected def recordPlan(plan: Option[Plan]): Unit = {
    //println(s"Recorded plan option $plan")
    this.planOption = plan
  }

  /**
    * Return the plan or throw an exception if none was recorded
    */
  def requiredPlan: jsPlan = {
    //println(s"Contents of recorded plan: $planOption")
    planOption.map(new jsPlan(_)).getOrElse(throw new IllegalArgumentException("No plan was recorded"))
  }

  /**
    * Return the plan or null if none was recorded
    */
  def plan: jsPlan = {
    planOption.map(new jsPlan(_)).orNull
  }

}

import scala.collection.JavaConverters._

/**
  * JavaScript-friendly version of Plan structure, without Scala collections and using null instead of Option
  */
class jsPlan(plan: Plan) {

  def messages: java.util.List[jsMessage] =
    plan.messages.map(new jsMessage(_)).asJava

}

class jsMessage(message: Message) {

  def body = message.body

}
