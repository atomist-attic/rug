package com.atomist.rug.runtime

import com.atomist.param.ParameterValue
import com.atomist.rug.spi.Plan.Plan

/**
  * For bot commands
  */
trait CommandHandler extends ParameterizedHandler {
  def intent: Seq[String]
  def handle(ctx: CommandContext, params: Seq[ParameterValue] = Seq()) : Option[Plan]
}
