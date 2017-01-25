package com.atomist.rug.runtime

import com.atomist.param.ParameterValues
import com.atomist.rug.spi.Handlers.Plan

/**
  * For bot commands
  */
trait CommandHandler extends ParameterizedRug {
  val intent: Seq[String]
  def handle(ctx: CommandContext, params: ParameterValues) : Option[Plan]
}
