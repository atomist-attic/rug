package com.atomist.rug.runtime.js

import com.atomist.event.{HandlerContext, ModelContextAware, SystemEvent, SystemEventHandler}
import com.atomist.param.Tag
import com.atomist.plan.TreeMaterializer
import com.atomist.rug.RugRuntimeException
import com.atomist.rug.kind.service.{ServiceSource, ServicesMutableView}
import com.atomist.rug.runtime.js.interop.{ContextMatch, PathExpressionExposer}
import com.atomist.source.ArtifactSource
import com.atomist.tree.content.text.SimpleMutableContainerTreeNode
import com.atomist.tree.pathexpression.{NamedNodeTest, PathExpressionParser}
import jdk.nashorn.api.scripting.ScriptObjectMirror

class JavaScriptEventHandler(
                              pathExpressionStr: String,
                              handlerFunction: ScriptObjectMirror,
                              rugAs: ArtifactSource,
                              materializer: TreeMaterializer,
                              pexe: PathExpressionExposer
                                 )
  extends SystemEventHandler {

  override def name: String = pathExpressionStr

  override def tags: Seq[Tag] = Nil

  override def description: String = name

  val pathExpression = PathExpressionParser.parsePathExpression(pathExpressionStr)

  override val rootNodeName: String = pathExpression.elements.head.test match {
    case nnt: NamedNodeTest => nnt.name
    case x => throw new IllegalArgumentException(s"Cannot start path expression without root node")
  }

  import com.atomist.tree.pathexpression.ExpressionEngine.NodePreparer

  private def nodePreparer(hc: HandlerContext): NodePreparer = {
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
    pexe.ee.evaluateParsed(root, pathExpression, Some(np)) match {
      case Right(Nil) =>
        println("Nothing to do: No nodes found")
      case Right(matches) =>
        val cm = ContextMatch(
          targetNode,
          //matches.map(m => m.asInstanceOf[Object]).asJava,
          pexe.wrap(matches),
          s2,
          teamId = e.teamId)
        invokeHandlerFunction(e, cm)
      //as.persist()
      case Left(failure) =>
        throw new RugRuntimeException(pathExpressionStr,
          s"Error evaluating path expression $pathExpression: [$failure]")
    }
  }

  protected def invokeHandlerFunction(e: SystemEvent, cm: ContextMatch): Unit = {
    //val context = new ResultsMutableView(rugAs, hc.servicesMutableView, as)
    val args = Seq(cm)

    // Signature is
    // on<R,N>(pathExpression: string, handler: (m: ContextMatch<R,N>) => void): void

      handlerFunction.call("apply", args:_*)
  }
}
