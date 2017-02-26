package com.atomist.rug.test.gherkin

import gherkin.ast.{Feature, ScenarioDefinition}

sealed trait Result

object Result {

  def apply(f: Boolean, s: String): Result = {
    if (f) Passed
    else Failed(s)
  }
}

case object Passed extends Result

case class Failed(why: String) extends Result

case object NotYetImplemented extends Result

trait TestRun {

  def result: Result
}

abstract class MultiTestRun(results: Seq[TestRun]) extends TestRun {

  override def result: Result =
    if (results.forall(_.result == Passed)) Passed
    else {
      val r = results.find(_.result.isInstanceOf[Failed])
        .map(_.result)
      r match {
        case Some(Failed(why)) => Failed(why)
        case _ => NotYetImplemented
      }
    }
}

case class AssertionResult(assertion: String, result: Result) extends TestRun

case class ScenarioResult(scenario: ScenarioDefinition, results: Seq[AssertionResult], data: String) extends MultiTestRun(results)

case class FeatureResult(f: Feature, scenarioResults: Seq[ScenarioResult]) extends MultiTestRun(scenarioResults)

case class TestResult(featureResults: Seq[FeatureResult]) extends MultiTestRun(featureResults)