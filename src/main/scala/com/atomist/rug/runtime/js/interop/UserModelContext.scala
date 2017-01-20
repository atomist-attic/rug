package com.atomist.rug.runtime.js.interop

import com.atomist.plan.{IdentityTreeMaterializer, TreeMaterializer}
import com.atomist.rug.kind.service.{ConsoleMessageBuilder, EmptyActionRegistry, MessageBuilder, TeamContext}

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
  * Entry point to Atomist system
  */
trait AtomistFacade extends UserModelContext with TeamContext {

  def on(s: String, handler: Any): Unit

  def messageBuilder: MessageBuilder

}

class DefaultAtomistFacade(
                            val teamId: String,
                            override val treeMaterializer: TreeMaterializer = IdentityTreeMaterializer)
  extends AtomistFacade {

  def on(s: String, handler: Any): Unit = {
    throw new UnsupportedOperationException("Event registration not supported")
  }

  override val registry = Map(
    "PathExpressionEngine" -> new jsPathExpressionEngine(this)
  )

  override def messageBuilder: MessageBuilder =
    new ConsoleMessageBuilder(teamId, EmptyActionRegistry)
}

/**
  * Used for Project editing only, when team id isn't needed
  */
object LocalAtomistFacade
  extends DefaultAtomistFacade("PROJECT_ONLY")