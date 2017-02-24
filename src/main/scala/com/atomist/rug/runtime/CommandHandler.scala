package com.atomist.rug.runtime

import com.atomist.param.ParameterValues
import com.atomist.rug.runtime.js.RugContext
import com.atomist.rug.spi.Handlers.Plan
import com.atomist.rug.spi.{MappedParameterizedRug, SecretAwareRug}

/**
  * For bot commands
  */
trait CommandHandler
  extends SecretAwareRug
    with MappedParameterizedRug {

  def intent: Seq[String]

  def handle(ctx: RugContext, params: ParameterValues): Option[Plan]
}
