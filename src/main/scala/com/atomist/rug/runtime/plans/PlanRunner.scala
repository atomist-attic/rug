package com.atomist.rug.runtime.plans

import com.atomist.rug.spi.Handlers
import com.atomist.rug.spi.Handlers.PlanResult

import scala.concurrent.Future

/**
  * Run a plan's instructions and send its messages
  */
trait PlanRunner {
  def run(plan: Handlers.Plan, callbackInput: AnyRef): Future[PlanResult]
}
