package com.atomist.rug.runtime

import com.atomist.param.ParameterValues
import com.atomist.rug.runtime.js.interop.RugContext
import com.atomist.rug.spi.Handlers.Plan

/**
  * For bot commands
  */
trait CommandHandler extends ParameterizedRug {
  val intent: Seq[String]
  def handle(ctx: RugContext, params: ParameterValues) : Option[Plan]
}
