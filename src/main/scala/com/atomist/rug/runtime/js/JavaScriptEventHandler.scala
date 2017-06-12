package com.atomist.rug.runtime.js

import com.atomist.graph.GraphNode
import com.atomist.param.Tag
import com.atomist.project.archive.RugResolver
import com.atomist.rug.runtime.js.interop._
import com.atomist.rug.runtime.js.nashorn.{NashornJavaScriptArray, jsSafeCommittingProxy, jsScalaHidingProxy}
import com.atomist.rug.runtime.{EventHandler, SystemEvent}
import com.atomist.rug.spi.Handlers.Plan
import com.atomist.rug.spi.{Secret, TypeRegistry}
import com.atomist.rug.{InvalidHandlerResultException, RugRuntimeException}
import com.atomist.tree.TreeNode
import com.atomist.tree.pathexpression.{NamedNodeTest, NodesWithTag, PathExpression, PathExpressionParser}

/**
  * Discover JavaScriptEventHandlers from a Nashorn instance
  */
class JavaScriptEventHandlerFinder
  extends JavaScriptRugFinder[JavaScriptEventHandler]
    with JavaScriptUtils {

  /**
    * Is the supplied thing valid at all?
    */
  def isValid(obj: JavaScriptObject): Boolean = {
    obj.getMember("__kind") == "event-handler" &&
      obj.hasMember("handle") &&
      obj.getMember("handle").asInstanceOf[JavaScriptObject].isFunction
  }

  override def create(jsc: JavaScriptEngineContext, someVar: JavaScriptObject, resolver: Option[RugResolver]): Option[JavaScriptEventHandler] = {
    Option(someVar.getMember("__expression")).map(v => {
      val expression: String = v.asInstanceOf[String]
      new JavaScriptEventHandler(jsc, someVar, expression, name(someVar), description(someVar), tags(someVar), secrets(someVar))
    })
  }
}

object JavaScriptEventHandler {

  def rootNodeFor(targetNode: GraphNode): GraphNode =
    SimpleContainerGraphNode("root", targetNode, TreeNode.Dynamic)
}

/**
  * An invokable JS based handler for System Events
  */
class JavaScriptEventHandler(jsc: JavaScriptEngineContext,
                             val handler: JavaScriptObject,
                             val pathExpressionStr: String,
                             override val name: String,
                             override val description: String,
                             override val tags: Seq[Tag],
                             override val secrets: Seq[Secret])
  extends EventHandler
    with JavaScriptUtils {

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
    ctx.pathExpressionEngine.ee.evaluate(root, pathExpression, ctx, None) match {
      case Right(Nil) => None
      case Right(matches) if matches.nonEmpty =>
        val cm = jsContextMatch(
          wrapOne(targetNode, ctx.typeRegistry),
          wrap(matches, ctx.typeRegistry),
          ctx.pathExpressionEngine,
          ctx.contextRoot(),
          teamId = e.teamId)
        jsc.invokeMember(
          handler,
          "handle",
          None,
          cm
        ) match {
          case plan: JavaScriptObject => ConstructPlan(plan, Some(this))
          case other => throw new InvalidHandlerResultException(s"$name EventHandler returned an invalid response ($other) when handling $pathExpressionStr")
        }
      case Right(matches) if matches.isEmpty => None
      case Left(failure) =>
        throw new RugRuntimeException(pathExpressionStr,
          s"Error evaluating path expression $pathExpression: [$failure]")
    }
  }

  private def wrapOne(n: GraphNode, typeRegistry: TypeRegistry): AnyRef = n match {
    case nbgn: JavaScriptBackedGraphNode =>
      nbgn.scriptObject.getNativeObject
    case _ =>
      jsSafeCommittingProxy.wrapOne(n, typeRegistry)
  }

  import scala.collection.JavaConverters._

  private def wrap(nodes: Seq[GraphNode], typeRegistry: TypeRegistry): java.util.List[AnyRef] =
    NashornJavaScriptArray(nodes.map(wrapOne(_, typeRegistry)))
}
