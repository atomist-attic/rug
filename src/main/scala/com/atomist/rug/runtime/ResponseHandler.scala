package com.atomist.rug.runtime

import com.atomist.param.ParameterValues
import com.atomist.rug.spi.Handlers.{Plan, Response}

/**
  * Handles responses from other Rugs & Executions
  */
trait ResponseHandler extends ParameterizedRug{
  def handle(response: Response, params: ParameterValues) : Option[Plan]
}
