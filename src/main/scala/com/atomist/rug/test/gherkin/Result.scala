package com.atomist.rug.test.gherkin

import gherkin.ast.{Feature, ScenarioDefinition}

/**
  * Result of one or more tests
  */
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

/**
  * Consistent interface for every layer of tests
  */
trait TestRun {

  def result: Result

  def passed: Boolean = result == Passed

  def testCount: Int
}

abstract class MultiTestRun(results: Seq[TestRun]) extends TestRun {

  override def testCount: Int = results.size

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

case class AssertionResult(assertion: String, result: Result) extends TestRun {

  override def testCount: Int = 1
}

case class ScenarioResult(scenario: ScenarioDefinition, results: Seq[AssertionResult], data: String) extends MultiTestRun(results)

case class FeatureResult(f: Feature, scenarioResults: Seq[ScenarioResult]) extends MultiTestRun(scenarioResults)

/**
  * Result of running tests for all features in an archive
  * @param featureResults results for each feature in the archive
  */
case class ArchiveTestResult(featureResults: Seq[FeatureResult]) extends MultiTestRun(featureResults)