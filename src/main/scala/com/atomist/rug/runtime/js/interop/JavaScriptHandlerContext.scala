package com.atomist.rug.runtime.js.interop

import com.atomist.plan.TreeMaterializer
import com.atomist.rug.kind.service.{MessageBuilder, TeamContext}
import com.atomist.tree.pathexpression.PathExpressionEngine

class JavaScriptHandlerContext(val teamId: String,
                               _treeMaterializer: TreeMaterializer,
                               _messageBuilder: MessageBuilder)

  extends UserModelContext with TeamContext {

  val pathExpressionEngine = new jsPathExpressionEngine(teamContext = this, ee = new PathExpressionEngine)

  val messageBuilder: MessageBuilder = _messageBuilder

  override val treeMaterializer: TreeMaterializer = _treeMaterializer

  override def registry: Map[String, Object] =  Map(
    "PathExpressionEngine" -> pathExpressionEngine
  )
}
