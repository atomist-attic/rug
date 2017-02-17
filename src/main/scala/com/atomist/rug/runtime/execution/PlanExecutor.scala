package com.atomist.rug.runtime.execution

import com.atomist.rug.spi.Handlers
import com.atomist.rug.spi.Handlers.PlanResult

import scala.concurrent.Future

trait PlanExecutor {
  def execute(plan: Handlers.Plan, callbackInput: AnyRef): Future[PlanResult]
}
