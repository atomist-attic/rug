package com.atomist.rug.test.gherkin.handler.event

import com.atomist.graph.GraphNode
import com.atomist.project.archive.Rugs
import com.atomist.rug.RugNotFoundException
import com.atomist.rug.runtime.js.RugContext
import com.atomist.rug.runtime.js.interop.NashornMapBackedGraphNode
import com.atomist.rug.runtime.{EventHandler, SystemEvent}
import com.atomist.rug.test.gherkin.Definitions
import com.atomist.rug.test.gherkin.handler.AbstractHandlerScenarioWorld
import com.atomist.tree.TreeMaterializer
import com.atomist.tree.pathexpression.PathExpression

class EventHandlerScenarioWorld(definitions: Definitions, rugs: Option[Rugs] = None)
  extends AbstractHandlerScenarioWorld(definitions, rugs) {

  //private var registeredHandlers: Set[EventHandler] = Set()

  private var handler: Option[EventHandler] = _

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

  def registerHandler(name: String): EventHandler = {
    val eh = eventHandler(name)
    handler = Some(eh)
    eh
  }

  /**
    * It's hopefully a ScriptObjectMirror
    */
  def sendEvent(e: AnyRef): Unit = {
    //println(s"Received event [$e]")
    val gn = NashornMapBackedGraphNode.toGraphNode(e).getOrElse(
      throw new IllegalArgumentException(s"Cannot make a GraphNode out of $e")
    )
    val h = handler.getOrElse(throw new IllegalStateException("No handler is registered"))
    if (gn.nodeTags.contains(h.rootNodeName)) {
      val tm = new TreeMaterializer {
        override def rootNodeFor(e: SystemEvent, pe: PathExpression) = gn
        override def hydrate(teamId: String, rawRootNode: GraphNode, pe: PathExpression) = rawRootNode
      }
      val rugContext: RugContext = createRugContext(tm)
      //println("About to handle event")
      recordPlan(h.handle(rugContext, SystemEvent(rugContext.teamId, h.rootNodeName, 1)))
    }
    else {
      // TODO should publish an event
      println(s"Handler $h handles [${h.rootNodeName}], not ${gn.nodeTags}")
    }
  }

}

