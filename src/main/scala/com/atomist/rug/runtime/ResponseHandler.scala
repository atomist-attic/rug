package com.atomist.rug.runtime

import com.atomist.param.ParameterValue
import com.atomist.rug.spi.Plan.Plan

/**
  * Handles responses from other Rugs & Executions
  */
trait ResponseHandler[T >: Serializable] extends ParameterizedHandler{
  def handle(response: InstructionResponse[T], params: Seq[ParameterValue] = Seq()) : Option[Plan]
}
