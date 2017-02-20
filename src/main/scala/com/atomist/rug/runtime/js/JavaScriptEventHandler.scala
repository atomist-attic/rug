package com.atomist.rug.runtime.js

import com.atomist.graph.GraphNode
import com.atomist.param.Tag
import com.atomist.rug.{InvalidHandlerResultException, RugRuntimeException}
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.runtime.js.interop.{JavaScriptHandlerContext, jsContextMatch, jsSafeCommittingProxy}
import com.atomist.rug.runtime.js.interop.{JavaScriptHandlerContext, jsContextMatch, jsPathExpressionEngine, jsSafeCommittingProxy}
import com.atomist.rug.runtime.{EventHandler, SystemEvent}
import com.atomist.rug.spi.Handlers.Plan
import com.atomist.source.ArtifactSource
import com.atomist.tree.content.text.SimpleMutableContainerTreeNode
import com.atomist.tree.pathexpression.{NamedNodeTest, NodesWithTag, PathExpression, PathExpressionParser}
import jdk.nashorn.api.scripting.ScriptObjectMirror

/**
  * Discover JavaScriptEventHandlers from a Nashorn instance
  */
class JavaScriptEventHandlerFinder(ctx: JavaScriptHandlerContext)
  extends BaseJavaScriptHandlerFinder[JavaScriptEventHandler](ctx) {

  override def kind = "event-handler"

  override def extractHandler(jsc: JavaScriptContext, someVar: ScriptObjectMirror, ctx: JavaScriptHandlerContext): Option[JavaScriptEventHandler] = {
    if(someVar.hasMember("__expression")){
      val expression: String = someVar.getMember("__expression").asInstanceOf[String]
      Some(new JavaScriptEventHandler(jsc, someVar, ctx, expression, name(someVar), description(someVar), tags(someVar)))
    } else {
      Option.empty
    }
  }
}

/**
  * An invokable JS based handler for System Events
  */
class JavaScriptEventHandler(jsc: JavaScriptContext,
                              handler: ScriptObjectMirror,
                              ctx: JavaScriptHandlerContext,
                              pathExpressionStr: String,
                              override val name: String,
                              override val description: String,
                              override val tags: Seq[Tag]
                                 )
  extends EventHandler
  with JavaScriptUtils {

  val pathExpression: PathExpression = PathExpressionParser.parsePathExpression(pathExpressionStr)

  override val rootNodeName: String = pathExpression.locationSteps.head.test match {
    case nnt: NamedNodeTest => nnt.name
    case nwt: NodesWithTag => nwt.tag
    case x => throw new IllegalArgumentException(s"Cannot start path expression without root node")
  }

  override def handle(e: SystemEvent): Option[Plan] = {

    val targetNode = ctx.treeMaterializer.rootNodeFor(e, pathExpression)
    // Put a new artificial root above to make expression work
    val root = new SimpleContainerGraphNode("root", targetNode)
    ctx.pathExpressionEngine.ee.evaluate(root, pathExpression, DefaultTypeRegistry, None) match {
      case Right(Nil) => None
      case Right(matches) =>
        val cm = jsContextMatch(
          jsSafeCommittingProxy.wrapOne(targetNode),
          jsSafeCommittingProxy.wrap(matches),
          teamId = e.teamId)
        invokeMemberFunction(jsc, handler, "handle", jsMatch(cm)) match {
          case plan: ScriptObjectMirror => ConstructPlan(plan)
          case other => throw new InvalidHandlerResultException(s"$name EventHandler returned an invalid response ($other) when handling $pathExpressionStr")

        }

      case Left(failure) =>
        throw new RugRuntimeException(pathExpressionStr,
          s"Error evaluating path expression $pathExpression: [$failure]")
    }
  }

}

case class SimpleContainerGraphNode(nodeName: String, child: GraphNode) extends GraphNode {

  override def relatedNodes: Seq[GraphNode] = Seq(child)

  override def relatedNodeNames: Set[String] = Set(child.nodeName)

  override def relatedNodeTypes: Set[String] = child.nodeTags

  override def relatedNodesNamed(key: String): Seq[GraphNode] = relatedNodes.filter(n => n.nodeName == key)
}

/**
  * Represents an event that drives a handler
  *
  * @param cm the root node in the tree
  */
private case class jsMatch(cm: jsContextMatch) {
  def root(): Object = cm.root
  def matches(): java.util.List[jsSafeCommittingProxy] = cm.matches
}
