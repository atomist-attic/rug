package com.atomist.rug.runtime.execution

import com.atomist.rug.spi.Handlers
import com.atomist.rug.spi.Handlers.PlanResponse

trait PlanExecutor {
  def execute(plan: Handlers.Plan, callbackInput: AnyRef): PlanResponse
}
