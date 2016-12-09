package com.atomist.rug.runtime.js.interop

import com.atomist.rug.kind.service.{ConsoleMessageBuilder, EmptyActionRegistry, MessageBuilder}


/**
  * Context exposed to user JavaScript.
  */
trait UserModelContext {

  def registry: Map[String, Object]

}


/**
  * Entry point to Atomist system
  */
trait AtomistFacade extends UserModelContext {

  def on(s: String, handler: Any): Unit

  def messageBuilder: MessageBuilder

}


object DefaultAtomistFacade extends AtomistFacade {

  def on(s: String, handler: Any): Unit = {
    throw new UnsupportedOperationException("Event registration not supported")
  }

  override val registry = Map(
    "PathExpressionEngine" -> new PathExpressionExposer
  )

  override def messageBuilder: MessageBuilder =
    new ConsoleMessageBuilder("TEAM_ID", EmptyActionRegistry)
}