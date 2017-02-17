package com.atomist.rug.runtime.plans

import com.atomist.rug.spi.Handlers
import com.atomist.rug.spi.Handlers.PlanResult

import scala.concurrent.Future

trait PlanRunner {
  def run(plan: Handlers.Plan, callbackInput: AnyRef): Future[PlanResult]
}
