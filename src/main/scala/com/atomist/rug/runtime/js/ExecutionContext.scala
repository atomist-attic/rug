package com.atomist.rug.runtime.js

import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.RepoResolver
import com.atomist.rug.spi.TypeRegistry

/**
  * Context of this execution for a user. Includes their
  * specific type registry (possibly with added types),
  * and a RepoResolver (if available), that knows their credentials.
  */
trait ExecutionContext {

  def typeRegistry: TypeRegistry

  /**
    * Provide one to be able to resolve projects, e.g. from repos.
    */
  def repoResolver: Option[RepoResolver] = None
}

object DefaultExecutionContext extends ExecutionContext {

  override def typeRegistry: TypeRegistry = DefaultTypeRegistry
}

case class SimpleExecutionContext(
                                   typeRegistry: TypeRegistry,
                                   override val repoResolver: Option[RepoResolver] = None)
  extends ExecutionContext
