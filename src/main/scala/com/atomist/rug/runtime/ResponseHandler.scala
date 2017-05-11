package com.atomist.rug.runtime

import com.atomist.param.ParameterValues
import com.atomist.rug.runtime.js.RugContext
import com.atomist.rug.spi.Handlers.{Plan, Response}

/**
  * Handles responses from other Rugs & Executions
  */
trait ResponseHandler extends ParameterizedRug{
  def handle(ctx: RugContext, response: Response, params: ParameterValues) : Option[Plan]
}
