package com.atomist.rug.runtime.js

import com.atomist.graph.GraphNode
import com.atomist.param.Tag
import com.atomist.rug.runtime.js.interop.{NashornMapBackedGraphNode, jsContextMatch, jsSafeCommittingProxy}
import com.atomist.rug.runtime.{AddressableRug, EventHandler, RugSupport, SystemEvent}
import com.atomist.rug.spi.Handlers.Plan
import com.atomist.rug.spi.TypeRegistry
import com.atomist.rug.{InvalidHandlerResultException, RugRuntimeException}
import com.atomist.tree.TreeNode
import com.atomist.tree.pathexpression.{NamedNodeTest, NodesWithTag, PathExpression, PathExpressionParser}
import com.atomist.util.lang.JavaScriptArray
import jdk.nashorn.api.scripting.ScriptObjectMirror

/**
  * Discover JavaScriptEventHandlers from a Nashorn instance
  */
class JavaScriptEventHandlerFinder
  extends BaseJavaScriptHandlerFinder[JavaScriptEventHandler] {

  override def kind = "event-handler"

  override def extractHandler(jsc: JavaScriptContext, someVar: ScriptObjectMirror, externalContext: Seq[AddressableRug]): Option[JavaScriptEventHandler] = {
    if (someVar.hasMember("__expression")) {
      val expression: String = someVar.getMember("__expression").asInstanceOf[String]
      Some(new JavaScriptEventHandler(jsc, someVar, expression, name(someVar), description(someVar), tags(someVar), externalContext))
    } else {
      Option.empty
    }
  }
}

object JavaScriptEventHandler {

  def rootNodeFor(targetNode: GraphNode): GraphNode =
    SimpleContainerGraphNode("root", targetNode, TreeNode.Dynamic)
}

/**
  * An invokable JS based handler for System Events
  */
class JavaScriptEventHandler(jsc: JavaScriptContext,
                             val handler: ScriptObjectMirror,
                             val pathExpressionStr: String,
                             override val name: String,
                             override val description: String,
                             override val tags: Seq[Tag],
                             override val externalContext: Seq[AddressableRug]
                            )
  extends EventHandler
    with JavaScriptUtils
    with RugSupport {

  val pathExpression: PathExpression = PathExpressionParser.parsePathExpression(pathExpressionStr)

  override val rootNodeName: String = pathExpression.locationSteps.head.test match {
    case nnt: NamedNodeTest => nnt.name
    case nwt: NodesWithTag => nwt.tag
    case _ => throw new IllegalArgumentException(s"Cannot start path expression without root node")
  }

  override def handle(ctx: RugContext, e: SystemEvent): Option[Plan] = {
    val targetNode = ctx.treeMaterializer.rootNodeFor(e, pathExpression)
    // Put a new artificial root above to make expression work
    val root = JavaScriptEventHandler.rootNodeFor(targetNode)
    ctx.pathExpressionEngine.ee.evaluate(root, pathExpression, ctx.typeRegistry, None) match {
      case Right(Nil) => None
      case Right(matches) if matches.nonEmpty =>
        val cm = jsContextMatch(
          wrapOne(targetNode, ctx.typeRegistry),
          wrap(matches, ctx.typeRegistry),
          teamId = e.teamId)
        invokeMemberFunction(jsc, handler, "handle", jsMatch(cm)) match {
          case plan: ScriptObjectMirror => ConstructPlan(plan)
          case other => throw new InvalidHandlerResultException(s"$name EventHandler returned an invalid response ($other) when handling $pathExpressionStr")
        }
      case Right(matches) if matches.isEmpty => None
      case Left(failure) =>
        throw new RugRuntimeException(pathExpressionStr,
          s"Error evaluating path expression $pathExpression: [$failure]")
    }
  }

  private def wrapOne(n: GraphNode, typeRegistry: TypeRegistry): AnyRef = n match {
    case nbgn: NashornMapBackedGraphNode =>
      nbgn.scriptObject
    case _ =>
      jsSafeCommittingProxy.wrapOne(n, typeRegistry)
  }

  import scala.collection.JavaConverters._

  private def wrap(nodes: Seq[GraphNode], typeRegistry: TypeRegistry): java.util.List[AnyRef] =
    new JavaScriptArray(nodes.map(wrapOne(_, typeRegistry)).asJava)
}

/**
  * Represents an event that drives a handler.
  *
  * @param cm the root node in the tree
  */
private case class jsMatch(cm: jsContextMatch) {

  def root(): Object = cm.root

  def matches(): java.util.List[AnyRef] = cm.matches

  override def toString: String = cm.toString
}
