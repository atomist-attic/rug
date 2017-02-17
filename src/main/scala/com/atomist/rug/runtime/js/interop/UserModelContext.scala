package com.atomist.rug.runtime.js.interop

import com.atomist.plan.{IdentityTreeMaterializer, TreeMaterializer}

/**
  * Context exposed to user JavaScript.
  */
trait UserModelContext {

  def registry: Map[String, Object]

}

/**
  * Services available to all JavaScript operations, whether
  * Editors or Executors or Handlers etc.
  */
trait UserServices {

  def pathExpressionEngine: jsPathExpressionEngine

}

/**
  * Information available for invocation within a team
  */
trait TeamContext {

  /**
    * Id of the team we're working on behalf of
    */
  def teamId: String

  /**
    * Used to hydrate nodes before running a path expression
    */
  def treeMaterializer: TreeMaterializer = IdentityTreeMaterializer

}
class DefaultAtomistContext(
                            val teamId: String,
                            override val treeMaterializer: TreeMaterializer = IdentityTreeMaterializer)
  extends UserModelContext with TeamContext {

  def on(s: String, handler: Any): Unit = {
    throw new UnsupportedOperationException("Event registration not supported")
  }

  override val registry = Map(
    "PathExpressionEngine" -> new jsPathExpressionEngine(this)
  )
}

/**
  * Used for Project editing only, when team id isn't needed
  */
object LocalAtomistContext
  extends DefaultAtomistContext("PROJECT_ONLY")
