package com.atomist.rug.runtime.js.interop

import com.atomist.rug.kind.service.{ConsoleMessageBuilder, EmptyActionRegistry, MessageBuilder}


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
trait AtomistFacade extends UserModelContext {

  def on(s: String, handler: Any): Unit

  def messageBuilder: MessageBuilder

}


object DefaultAtomistFacade extends AtomistFacade {

  def on(s: String, handler: Any): Unit = {
    throw new UnsupportedOperationException("Event registration not supported")
  }

  override val registry = Map(
    "PathExpressionEngine" -> new jsPathExpressionEngine
  )

  override def messageBuilder: MessageBuilder =
    new ConsoleMessageBuilder("TEAM_ID", EmptyActionRegistry)
}