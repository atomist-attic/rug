package com.atomist.rug.runtime.plans

import com.atomist.rug.spi.Handlers.Instruction.{Detail, Edit}
import com.atomist.rug.spi.Handlers.Status.{Failure, Success}
import com.atomist.rug.spi.Handlers._
import org.mockito.Mockito._
import org.scalatest._
import org.scalatest.mockito.MockitoSugar
import org.slf4j.Logger

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class PlanResultLoggerTest extends FunSpec with Matchers with DiagrammedAssertions with OneInstancePerTest with MockitoSugar {

  val mockLogger = mock[Logger]
  val planResultLogger = new PlanResultLogger(mockLogger)

  val successfulInstructionResult = InstructionResult(Edit(Detail("edit1", None, Nil, None)), Response(Success))
  val failureInstructionResult = InstructionResult(Edit(Detail("edit2", None, Nil, None)), Response(Failure))
  val errorInstructionResult = InstructionError(Edit(Detail("edit3", None, Nil, None)), new IllegalStateException("doh!"))
  val successfulNestedPlan = NestedPlanRun(Plan(Nil, Nil), Future {PlanResult(Seq(successfulInstructionResult))})
  val failureNestedPlan = NestedPlanRun(Plan(Nil, Nil), Future {PlanResult(Seq(failureInstructionResult))})
  val errorNestedPlan = NestedPlanRun(Plan(Nil, Nil), Future {PlanResult(Seq(errorInstructionResult))})

  it ("should interpret empty plan result as success") {
    val planResult = PlanResult(Nil)
    planResultLogger.log(planResult)
    verifyNoMoreInteractions(mockLogger)
  }

  it ("should interpret plan result with successful instruction responses as success") {
    val planResult = PlanResult(Seq(
      successfulInstructionResult
    ))
    planResultLogger.log(planResult)
    verifyNoMoreInteractions(mockLogger)
  }

  it ("should interpret plan result with failure response as failure") {
    val planResult = PlanResult(Seq(
      failureInstructionResult
    ))
    planResultLogger.log(planResult)
    verify(mockLogger).error("Failure running plan.", failureInstructionResult)
    verifyNoMoreInteractions(mockLogger)
  }

  it ("should interpret plan result with exception as failure") {
    val planResult = PlanResult(Seq(
      errorInstructionResult
    ))
    planResultLogger.log(planResult)
    verify(mockLogger).error("Error running plan.", errorInstructionResult.error)
    verifyNoMoreInteractions(mockLogger)
  }

  it ("should interpret plan result with success and failure responses as failure") {
    val planResult = PlanResult(Seq(
      successfulInstructionResult,
      failureInstructionResult
    ))
    planResultLogger.log(planResult)
    verify(mockLogger).error("Failure running plan.", failureInstructionResult)
    verifyNoMoreInteractions(mockLogger)
  }

  it ("should interpret plan result with success response and exception as failure") {
    val planResult = PlanResult(Seq(
      successfulInstructionResult,
      errorInstructionResult
    ))
    planResultLogger.log(planResult)
    verify(mockLogger).error("Error running plan.", errorInstructionResult.error)
    verifyNoMoreInteractions(mockLogger)
  }

  it ("should interpret plan result with successful nested plan as success") {
    val planResult = PlanResult(Seq(
      successfulInstructionResult,
      successfulNestedPlan
    ))
    planResultLogger.log(planResult)
    verifyNoMoreInteractions(mockLogger)
  }

  it ("should interpret plan result with failure nested plan as failure") {
    val planResult = PlanResult(Seq(
      successfulInstructionResult,
      failureNestedPlan
    ))
    planResultLogger.log(planResult)
    verify(mockLogger).error("Failure running plan.", failureInstructionResult)
    verifyNoMoreInteractions(mockLogger)
  }

  it ("should interpret plan result with error nested plan as failure") {
    val planResult = PlanResult(Seq(
      successfulInstructionResult,
      errorNestedPlan
    ))
    planResultLogger.log(planResult)
    verify(mockLogger).error("Error running plan.", errorInstructionResult.error)
    verifyNoMoreInteractions(mockLogger)
  }
}
