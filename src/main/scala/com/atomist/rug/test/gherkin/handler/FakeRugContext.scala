package com.atomist.rug.test.gherkin.handler

import com.atomist.rug.runtime.js.RugContext
import com.atomist.rug.runtime.js.interop.jsPathExpressionEngine
import com.atomist.rug.spi.{TypeRegistry, Typed}
import com.atomist.tree.TreeMaterializer

class FakeRugContext(val teamId: String, tr: TypeRegistry, _treeMaterializer: TreeMaterializer) extends RugContext {

  private var jsee = new jsPathExpressionEngine(this, typeRegistry = tr)

  override val pathExpressionEngine: jsPathExpressionEngine = jsee

  override def treeMaterializer: TreeMaterializer = _treeMaterializer

  def registerType(t: Typed): Unit = {
    jsee = jsee.addType(t)
  }

}
