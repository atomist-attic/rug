package com.atomist.rug.runtime.plans

import com.atomist.rug.spi.Handlers.Status.Failure
import com.atomist.rug.spi.Handlers._
import org.slf4j.Logger

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class PlanResultLogger(val logger: Logger) {

  def log(planResult: PlanResult): Unit = {
    logEvents(planResult.log)
  }

  @tailrec
  private def logEvents(log: Seq[PlanLogEvent]): Unit = {
    log.headOption match {
      case Some(head) =>
        val remainingEvents = head match {
          case logError: PlanLogError =>
            logger.error("Error running plan.", logError.error)
            log.tail
          case result: InstructionResult if result.response.status == Failure =>
            logger.error("Failure running plan.", result)
            log.tail
          case result: NestedPlanRun =>
            val planResult = Await.result(result.planResult, 5.minutes)
            log.tail ++ planResult.log
          case _ => log.tail
        }
        logEvents(remainingEvents)
      case None =>
    }
  }
}
