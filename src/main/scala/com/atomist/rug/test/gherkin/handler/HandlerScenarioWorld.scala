package com.atomist.rug.test.gherkin.handler

import com.atomist.project.archive.Rugs
import com.atomist.rug.RugNotFoundException
import com.atomist.rug.runtime.{CommandHandler, EventHandler}
import com.atomist.rug.runtime.js.RugContext
import com.atomist.rug.test.gherkin.{Definitions, ScenarioWorld}

class HandlerScenarioWorld(definitions: Definitions, rugContext: RugContext, rugs: Option[Rugs] = None)
  extends ScenarioWorld(definitions, rugs) {

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

  def eventHandler(name: String): EventHandler = {
    rugs match {
      case Some(r) =>
        r.eventHandlers.find(e => e.name == name) match {
          case Some(e) => e
          case _ => throw new RugNotFoundException(
            s"EventHandler with name '$name' can not be found in current context. Known EventHandlers are [${r.eventHandlerNames.mkString(", ")}]")
        }
      case _ => throw new RugNotFoundException("No context provided")
    }
  }

  def invokeHandler(handler: CommandHandler, params: Any): Unit = {
    val resultAs = handler.handle(rugContext, parameters(params))
  }

}
