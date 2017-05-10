package com.atomist.rug.runtime.js

import com.atomist.graph.GraphNode
import com.atomist.param.Tag
import com.atomist.project.archive.RugResolver
import com.atomist.rug.runtime.js.interop.{NashornMapBackedGraphNode, jsContextMatch, jsSafeCommittingProxy, jsScalaHidingProxy}
import com.atomist.rug.runtime.{EventHandler, SystemEvent}
import com.atomist.rug.spi.Handlers.Plan
import com.atomist.rug.spi.{Secret, TypeRegistry}
import com.atomist.rug.{InvalidHandlerResultException, RugRuntimeException}
import com.atomist.tree.TreeNode
import com.atomist.tree.pathexpression.{NamedNodeTest, NodesWithTag, PathExpression, PathExpressionParser}
import com.atomist.util.lang.JavaScriptArray
import jdk.nashorn.api.scripting.ScriptObjectMirror

/**
  * Discover JavaScriptEventHandlers from a Nashorn instance
  */
class JavaScriptEventHandlerFinder
  extends JavaScriptRugFinder[JavaScriptEventHandler]
    with JavaScriptUtils {

  /**
    * Is the supplied thing valid at all?
    */
  def isValid(obj: ScriptObjectMirror): Boolean = {
    obj.getMember("__kind") == "event-handler" &&
      obj.hasMember("handle") &&
      obj.getMember("handle").asInstanceOf[ScriptObjectMirror].isFunction
  }

  override def create(jsc: JavaScriptContext, someVar: ScriptObjectMirror, resolver: Option[RugResolver]): Option[JavaScriptEventHandler] = {
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
class JavaScriptEventHandler(jsc: JavaScriptContext,
                             val handler: ScriptObjectMirror,
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
        invokeMemberFunction(
          jsc,
          handler,
          "handle",
          None,
          jsScalaHidingProxy(cm, returnNotToProxy = _ => true)
        ) match {
          case plan: ScriptObjectMirror => ConstructPlan(plan, Some(this))
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

