package com.atomist.rug.runtime.js.interop

/**
  * Context exposed to user JavaScript.
  */
trait UserModelContext {

  def registry: Map[String, Object]

}

object DefaultUserModelContext extends UserModelContext {

  override val registry = Map(
    "PathExpressionEngine" -> new PathExpressionExposer
  )
}
