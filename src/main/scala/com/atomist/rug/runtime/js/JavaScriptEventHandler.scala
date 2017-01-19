package com.atomist.rug.runtime.js

import com.atomist.event.{HandlerContext, ModelContextAware, SystemEvent, SystemEventHandler}
import com.atomist.param.Tag
import com.atomist.plan.TreeMaterializer
import com.atomist.rug.RugRuntimeException
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.service.{ServiceSource, ServicesMutableView}
import com.atomist.rug.runtime.js.interop.{ContextMatch, jsPathExpressionEngine}
import com.atomist.source.ArtifactSource
import com.atomist.tree.content.text.SimpleMutableContainerTreeNode
import com.atomist.tree.pathexpression.{NamedNodeTest, PathExpression, PathExpressionParser}
import jdk.nashorn.api.scripting.ScriptObjectMirror

class JavaScriptEventHandler(
                              pathExpressionStr: String,
                              handlerFunction: ScriptObjectMirror,
                              rugAs: ArtifactSource,
                              materializer: TreeMaterializer,
                              pexe: jsPathExpressionEngine
                                 )
  extends SystemEventHandler {

  override def name: String = pathExpressionStr

  override def tags: Seq[Tag] = Nil

  override def description: String = name

  val pathExpression: PathExpression = PathExpressionParser.parsePathExpression(pathExpressionStr)

  override val rootNodeName: String = pathExpression.locationSteps.head.test match {
    case nnt: NamedNodeTest => nnt.name
    case x => throw new IllegalArgumentException(s"Cannot start path expression without root node")
  }

  import com.atomist.tree.pathexpression.ExpressionEngine.NodePreparer

  protected def nodePreparer(hc: HandlerContext): NodePreparer = {
    case mca: ModelContextAware =>
      mca.setContext(hc)
      mca
    case x => x
  }

  override def handle(e: SystemEvent, s2: ServiceSource): Unit = {
    val smv = new ServicesMutableView(rugAs, s2)
    val handlerContext = HandlerContext(smv)
    val np = nodePreparer(handlerContext)

    val targetNode = materializer.rootNodeFor(e, pathExpression)
    // Put a new artificial root above to make expression work
    val root = new SimpleMutableContainerTreeNode("root", Seq(targetNode), null, null)
    pexe.ee.evaluate(root, pathExpression, DefaultTypeRegistry, Some(np)) match {
      case Right(Nil) =>
      case Right(matches) =>
        val cm = ContextMatch(
          targetNode,
          pexe.wrap(matches),
          s2,
          teamId = e.teamId)
        invokeHandlerFunction(e, cm)
      case Left(failure) =>
        throw new RugRuntimeException(pathExpressionStr,
          s"Error evaluating path expression $pathExpression: [$failure]")
    }
  }

  protected def invokeHandlerFunction(e: SystemEvent, cm: ContextMatch): Object = {
    handlerFunction.call("apply", cm)
  }
}
