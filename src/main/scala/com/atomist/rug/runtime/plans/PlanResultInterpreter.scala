package com.atomist.rug.runtime.plans

import com.atomist.rug.spi.Handlers.Status.{Failure, Success}
import com.atomist.rug.spi.Handlers._

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class PlanResultInterpreter {

  def interpret(planResult: PlanResult): Response = {
    if (hasLogFailure(planResult.log)) {
      Response(Failure)
    } else {
      Response(Success)
    }
  }

  @tailrec
  private def hasLogFailure(log: Seq[PlanLogEvent]): Boolean = {
    log.headOption match {
      case Some(head) =>
        head match {
          case _: PlanLogError => true
          case result: InstructionResult if result.response.status == Failure => true
          case result: NestedPlanRun =>
            val planResult = Await.result(result.planResult, 5.minutes)
            hasLogFailure(log.tail ++ planResult.log)
          case _ => hasLogFailure(log.tail)
        }
      case None => false
    }
  }

}
