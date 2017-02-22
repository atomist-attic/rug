package com.atomist.rug.runtime

import com.atomist.param.ParameterValues
import com.atomist.rug.runtime.js.RugContext
import com.atomist.rug.spi.Handlers.Plan
import com.atomist.rug.spi.SecretAwareRug

/**
  * For bot commands
  */
trait CommandHandler extends SecretAwareRug {
  val intent: Seq[String]
  def handle(ctx: RugContext, params: ParameterValues) : Option[Plan]
}
