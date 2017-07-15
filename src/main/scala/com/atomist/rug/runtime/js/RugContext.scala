package com.atomist.rug.runtime.js

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.runtime.js.interop.{jsGitProjectLoader, jsPathExpressionEngine}
import com.atomist.rug.spi.{ExportFunction, TypeRegistry}
import com.atomist.tree.{IdentityTreeMaterializer, TreeMaterializer}

/**
  * Services available to all operations, whether
  * CommandHandlers, EventHandlers or project operations.
  * Some of these are exposed to JavaScript
  * in the HandlerContext TypeScript interface.
  */
trait RugContext extends ExecutionContext {

  @ExportFunction(readOnly = true, description = "The path expression engine", exposeAsProperty = true)
  def pathExpressionEngine: jsPathExpressionEngine

  /**
    * Used to hydrate nodes before running a path expression
    */
  def treeMaterializer: TreeMaterializer = IdentityTreeMaterializer

  /**
    * Id of the team we're working on behalf of
    */
  @ExportFunction(readOnly = true, description = "Current team id", exposeAsProperty = true)
  def teamId: String

  /**
    * @return the root node for the team's context.
    *         Path expressions can be executed against it,
    *         for example in command handlers.
    *         Normally a GraphNode, but detyped to
    *         enable JVM/JS interop
    */
  @ExportFunction(readOnly = true, exposeAsProperty = true, description="Root node for team's context")
  def contextRoot(): AnyRef

  @ExportFunction(readOnly = true, description = "Load projects from git", exposeAsProperty = true)
  def gitProjectLoader: AnyRef = new jsGitProjectLoader(repoResolver)

}

class BaseRugContext extends RugContext {

  var _treeMaterializer: TreeMaterializer = IdentityTreeMaterializer

  override def typeRegistry: TypeRegistry = DefaultTypeRegistry


  override def treeMaterializer: TreeMaterializer = _treeMaterializer

  @ExportFunction(description = "The path expression engine", exposeAsProperty = true, readOnly = true)
  override def pathExpressionEngine: jsPathExpressionEngine = new jsPathExpressionEngine(this)

  /**
    * Id of the team we're working on behalf of
    */
  override def teamId: String = "LOCAL_CONTEXT"

  @ExportFunction(description = "The root context. Most likely a ChatTeam for now", exposeAsProperty = true, readOnly = true)
  override def contextRoot(): GraphNode = null
}

object LocalRugContext extends BaseRugContext {

  def apply(trees: TreeMaterializer): LocalRugContext.type = {
    this._treeMaterializer = trees
    this
  }
}
