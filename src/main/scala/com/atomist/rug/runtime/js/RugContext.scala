package com.atomist.rug.runtime.js

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.runtime.js.interop.jsPathExpressionEngine
import com.atomist.rug.spi.TypeRegistry
import com.atomist.tree.{IdentityTreeMaterializer, TreeMaterializer}


/**
  * Services available to all operations, whether
  * CommandHandlers, EventHandlers or project operations.
  * Some of these are exposed to JavaScript
  * in the HandlerContext TypeScript interface.
  */
trait RugContext {

  def typeRegistry: TypeRegistry

  def pathExpressionEngine: jsPathExpressionEngine

  /**
    * Used to hydrate nodes before running a path expression
    */
  def treeMaterializer: TreeMaterializer = IdentityTreeMaterializer

  /**
    * Id of the team we're working on behalf of
    */
  def teamId: String

  /**
    * @return the root node for the team's context.
    *         Path expressions can be executed against it,
    *         for example in command handlers
    */
  def contextRoot(): GraphNode

}


object LocalRugContext extends RugContext {

  var _treeMaterializer: TreeMaterializer = IdentityTreeMaterializer

  def apply(trees: TreeMaterializer): LocalRugContext.type = {
    this._treeMaterializer = trees
    this
  }

  override def typeRegistry: TypeRegistry = DefaultTypeRegistry

  override def treeMaterializer: TreeMaterializer = _treeMaterializer

  override def pathExpressionEngine: jsPathExpressionEngine = new jsPathExpressionEngine(this)

  /**
    * Id of the team we're working on behalf of
    */
  override def teamId: String = "LOCAL_CONTEXT"

  override def contextRoot(): GraphNode = ???
}
