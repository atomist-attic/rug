package com.atomist.rug.runtime

import com.atomist.param.ParameterValues
import com.atomist.rug.spi.Plan.Plan

/**
  * Handles responses from other Rugs & Executions
  */
trait ResponseHandler extends ParameterizedHandler{
  def handle(response: InstructionResponse, params: ParameterValues) : Option[Plan]
}
