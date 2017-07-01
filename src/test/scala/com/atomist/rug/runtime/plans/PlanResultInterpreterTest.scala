package com.atomist.rug.runtime.plans

import com.atomist.rug.spi.Handlers.Instruction.{Detail, Edit}
import com.atomist.rug.spi.Handlers.Status.{Failure, Success}
import com.atomist.rug.spi.Handlers._
import org.scalatest.{DiagrammedAssertions, FunSpec, Matchers, OneInstancePerTest}

import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class PlanResultInterpreterTest extends FunSpec with Matchers with DiagrammedAssertions with OneInstancePerTest  {

  val successfulInstructionResult = InstructionResult(Edit(Detail("edit1", None, Nil, None)), Response(Success))
  val failureInstructionResult = InstructionResult(Edit(Detail("edit2", None, Nil, None)), Response(Failure))
  val errorInstructionResult = InstructionError(Edit(Detail("edit3", None, Nil, None)), new IllegalStateException("doh!"))
  val successfulNestedPlan = NestedPlanRun(Plan(None,Nil,Nil, Nil), PlanResult(Seq(successfulInstructionResult)))
  val failureNestedPlan = NestedPlanRun(Plan(None,Nil, Nil, Nil), PlanResult(Seq(failureInstructionResult)))
  val errorNestedPlan = NestedPlanRun(Plan(None,Nil, Nil, Nil), PlanResult(Seq(errorInstructionResult)))

  it ("should interpret empty plan result as success") {
    val planResult = PlanResult(Nil)
    val actualResponse = PlanResultInterpreter.interpret(planResult)
    val expectedResponse = Response(Success)
    assert (actualResponse == expectedResponse)
  }

  it ("should interpret plan result with successful instruction responses as success") {
    val planResult = PlanResult(Seq(
      successfulInstructionResult
    ))
    val actualResponse = PlanResultInterpreter.interpret(planResult)
    val expectedResponse = Response(Success)
    assert (actualResponse == expectedResponse)
  }

  it ("should interpret plan result with failure response as failure") {
    val planResult = PlanResult(Seq(
      failureInstructionResult
    ))
    val actualResponse = PlanResultInterpreter.interpret(planResult)
    val expectedResponse = Response(Failure)
    assert (actualResponse == expectedResponse)
  }

  it ("should interpret plan result with exception as failure") {
    val planResult = PlanResult(Seq(
      errorInstructionResult
    ))
    val actualResponse = PlanResultInterpreter.interpret(planResult)
    val expectedResponse = Response(Failure)
    assert (actualResponse == expectedResponse)
  }

  it ("should interpret plan result with success and failure responses as failure") {
    val planResult = PlanResult(Seq(
      successfulInstructionResult,
      failureInstructionResult
    ))
    val actualResponse = PlanResultInterpreter.interpret(planResult)
    val expectedResponse = Response(Failure)
    assert (actualResponse == expectedResponse)
  }

  it ("should interpret plan result with success response and exception as failure") {
    val planResult = PlanResult(Seq(
      successfulInstructionResult,
      errorInstructionResult
    ))
    val actualResponse = PlanResultInterpreter.interpret(planResult)
    val expectedResponse = Response(Failure)
    assert (actualResponse == expectedResponse)
  }

  it ("should interpret plan result with successful nested plan as success") {
    val planResult = PlanResult(Seq(
      successfulInstructionResult,
      successfulNestedPlan
    ))
    val actualResponse = PlanResultInterpreter.interpret(planResult)
    val expectedResponse = Response(Success)
    assert (actualResponse == expectedResponse)
  }

  it ("should interpret plan result with failure nested plan as failure") {
    val planResult = PlanResult(Seq(
      successfulInstructionResult,
      failureNestedPlan
    ))
    val actualResponse = PlanResultInterpreter.interpret(planResult)
    val expectedResponse = Response(Failure)
    assert (actualResponse == expectedResponse)
  }

  it ("should interpret plan result with error nested plan as failure") {
    val planResult = PlanResult(Seq(
      successfulInstructionResult,
      errorNestedPlan
    ))
    val actualResponse = PlanResultInterpreter.interpret(planResult)
    val expectedResponse = Response(Failure)
    assert (actualResponse == expectedResponse)
  }

}
