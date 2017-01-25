package com.atomist.rug.runtime.js.interop

import com.atomist.plan.TreeMaterializer
import com.atomist.tree.pathexpression.PathExpressionEngine

class JavaScriptHandlerContext(val teamId: String,
                               _treeMaterializer: TreeMaterializer)

  extends UserModelContext with TeamContext {

  val pathExpressionEngine = new jsPathExpressionEngine(teamContext = this, ee = new PathExpressionEngine)

  override val treeMaterializer: TreeMaterializer = _treeMaterializer

  override def registry: Map[String, Object] =  Map(
    "PathExpressionEngine" -> pathExpressionEngine
  )
}
