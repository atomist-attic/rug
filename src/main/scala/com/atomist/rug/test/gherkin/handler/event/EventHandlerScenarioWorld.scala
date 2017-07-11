package com.atomist.rug.test.gherkin.handler.event

import com.atomist.graph.GraphNode
import com.atomist.project.archive.Rugs
import com.atomist.rug.RugNotFoundException
import com.atomist.rug.runtime.js.interop.JavaScriptBackedGraphNode
import com.atomist.rug.runtime.js.{JavaScriptEventHandler, RugContext}
import com.atomist.rug.runtime.{EventHandler, SystemEvent}
import com.atomist.rug.spi.ExportFunction
import com.atomist.rug.test.gherkin.{Definitions, GherkinExecutionListener, GherkinRunnerConfig, PathExpressionEvaluation}
import com.atomist.rug.test.gherkin.handler.AbstractHandlerScenarioWorld
import com.atomist.tree.TreeMaterializer
import com.atomist.tree.pathexpression.PathExpression

import scala.collection.mutable.ListBuffer

/**
  * World implementation for testing event handlers. Allows us to pump in events
  * and test the reaction
  */
class EventHandlerScenarioWorld(definitions: Definitions, rugs: Option[Rugs] = None, listeners: Seq[GherkinExecutionListener], config: GherkinRunnerConfig)
  extends AbstractHandlerScenarioWorld(definitions, rugs, listeners, config) {

  private val registeredHandlers = ListBuffer.empty[EventHandler]

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

  @ExportFunction(description = "Register a handler", readOnly = true)
  def registerHandler(name: String): EventHandler = {
    val eh = eventHandler(name)
    registeredHandlers.append(eh)
    eh
  }

  /**
    * Publish an event constructed in TypeScript using our API
    * (normally a cortex stub)
    * It's hopefully a JavaScriptObject
    */
  //@ExportFunction(description = "Send and event", readOnly = true)
  def sendEvent(e: AnyRef): Unit = {
    val gn = JavaScriptBackedGraphNode.toGraphNode(e).getOrElse(
      throw new IllegalArgumentException(s"Cannot make a GraphNode out of $e")
    )
    if (registeredHandlers.isEmpty)
      throw new IllegalStateException("No handler is registered")
    for (h <- registeredHandlers) {
      handleEventNode(gn, h)
    }
  }

  private def handleEventNode(eventNode: GraphNode, h: EventHandler) = {
    if (eventNode.nodeTags.contains(h.rootNodeName)) {
      val tm = new TreeMaterializer {
        override def rootNodeFor(e: SystemEvent, pe: PathExpression) = eventNode

        override def hydrate(teamId: String, rawRootNode: GraphNode, pe: PathExpression) = rawRootNode
      }
      val rugContext: RugContext = createRugContext(tm)

      // Check if it matches, if we can
      notifyListenersOfMatchResult(eventNode, h, rugContext)

      //println("About to handle event")
      val plan = h.handle(rugContext, SystemEvent(rugContext.teamId, h.rootNodeName, 1))
      plan.foreach(recordPlan(h.name, _))
    }
    else {
      notifyListenersOfMatchPossible(eventNode, h)
    }
  }

  private def notifyListenersOfMatchResult(eventNode: GraphNode, h: EventHandler, rugContext: RugContext) = {
    h match {
      case jsh: JavaScriptEventHandler =>
        rugContext.pathExpressionEngine.ee.evaluate(JavaScriptEventHandler.rootNodeFor(eventNode), jsh.pathExpression, rugContext) match {
          case Right(nodes) =>
            //println(s"Results for [${jsh.pathExpressionStr}] were ${nodes}")
            for (l <- listeners)
              l.pathExpressionResult(PathExpressionEvaluation(jsh.pathExpressionStr, eventNode, nodes))
          case Left(_) =>
          // The evaluation failed. Who cares. The test will blow up anyway
        }
      case _ =>
      // We can't find the path expression to check the match for
    }
  }

  private def notifyListenersOfMatchPossible(eventNode: GraphNode, h: EventHandler) = {
    h match {
      case jsh: JavaScriptEventHandler =>
        for (l <- listeners)
          l.pathExpressionResult(PathExpressionEvaluation(jsh.pathExpressionStr, eventNode, Nil))
      case _ =>
      // We can't find the path expression to check the match for
    }
  }

}
