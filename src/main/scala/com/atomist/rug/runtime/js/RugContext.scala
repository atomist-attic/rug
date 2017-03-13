package com.atomist.rug.runtime.js

import com.atomist.rug.runtime.js.interop.jsPathExpressionEngine
import com.atomist.rug.spi.TypeRegistry
import com.atomist.tree.{IdentityTreeMaterializer, TreeMaterializer}


/**
  * Services available to all JavaScript operations, whether
  * Editors or Executors or Handlers etc.
  */
trait RugContext {

  def typeRegistry: TypeRegistry = pathExpressionEngine.typeRegistry

  def pathExpressionEngine: jsPathExpressionEngine

  /**
    * Used to hydrate nodes before running a path expression
    */
  def treeMaterializer: TreeMaterializer = IdentityTreeMaterializer

  /**
    * Id of the team we're working on behalf of
    */
  def teamId: String
}


object LocalRugContext extends RugContext {

  var _treeMaterializer: TreeMaterializer = IdentityTreeMaterializer

  def apply(trees: TreeMaterializer): LocalRugContext.type = {
    this._treeMaterializer = trees
    this
  }

  override def treeMaterializer: TreeMaterializer = _treeMaterializer

  override def pathExpressionEngine: jsPathExpressionEngine = new jsPathExpressionEngine(this)

  /**
    * Id of the team we're working on behalf of
    */
  override def teamId: String = "LOCAL_CONTEXT"
}
