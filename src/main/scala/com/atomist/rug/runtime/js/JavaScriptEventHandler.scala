package com.atomist.rug.runtime.js

import com.atomist.param.Tag
import com.atomist.rug.RugRuntimeException
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.runtime.js.interop.jsPathExpressionEngine
import com.atomist.rug.runtime.{SystemEvent, SystemEventHandler}
import com.atomist.rug.runtime.js.interop.{JavaScriptHandlerContext, jsContextMatch}
import com.atomist.rug.spi.Plan.Plan
import com.atomist.source.ArtifactSource
import com.atomist.tree.content.text.SimpleMutableContainerTreeNode
import com.atomist.tree.pathexpression.{NamedNodeTest, PathExpression, PathExpressionParser}
import jdk.nashorn.api.scripting.ScriptObjectMirror

class JavaScriptEventHandler(
                              pathExpressionStr: String,
                              handlerFunction: ScriptObjectMirror,
                              thiz: ScriptObjectMirror,
                              rugAs: ArtifactSource,
                              ctx: JavaScriptHandlerContext,
                              _name: String,
                              _description: String,
                              _tags: Seq[Tag] = Nil
                                 )
  extends SystemEventHandler {

  override def name: String = _name

  override def tags: Seq[Tag] = Nil

  override def description: String = _description

  val pathExpression: PathExpression = PathExpressionParser.parsePathExpression(pathExpressionStr)

  override val rootNodeName: String = pathExpression.locationSteps.head.test match {
    case nnt: NamedNodeTest => nnt.name
    case x => throw new IllegalArgumentException(s"Cannot start path expression without root node")
  }

  override def handle(e: SystemEvent): Option[Plan] = {

    val targetNode = ctx.treeMaterializer.rootNodeFor(e, pathExpression)
    // Put a new artificial root above to make expression work
    val root = new SimpleMutableContainerTreeNode("root", Seq(targetNode), null, null)
    ctx.pathExpressionEngine.ee.evaluate(root, pathExpression, DefaultTypeRegistry, None) match {
      case Right(Nil) =>
      case Right(matches) =>
        val cm = jsContextMatch(
          targetNode,
          jsPathExpressionEngine.wrap(matches),
          teamId = e.teamId)
        //TODO wrap this in safe committing proxy
        handlerFunction.call(thiz, jsMatch(cm))

      case Left(failure) =>
        throw new RugRuntimeException(pathExpressionStr,
          s"Error evaluating path expression $pathExpression: [$failure]")
    }
    Some(Plan(Nil,Nil))
  }
}

/**
  * Represents an event that drives a handler
  *
  * @param cm the root node in the tree
  */
private case class jsMatch(cm: jsContextMatch) {
  def root(): Object = cm.root
  def matches(): java.util.List[Object] = cm.matches
}