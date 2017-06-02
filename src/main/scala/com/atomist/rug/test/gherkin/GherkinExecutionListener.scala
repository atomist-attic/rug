package com.atomist.rug.test.gherkin

import com.atomist.graph.GraphNode
import gherkin.ast.{ScenarioDefinition, Step}

/**
  * Simple listener trait that can be implemented to receive notifications during
  * test execution.
  */
trait GherkinExecutionListener {

  /**
    * Notifies about an immediate feature definition execution start
    *
    * @param feature feature definition that is about to be executed
    */
  def featureStarting(feature: FeatureDefinition)

  /**
    * Notifies about an immediate scenrio definition execution start
    *
    * @param scenario scenarion definition that is about to be executed
    */
  def scenarioStarting(scenario: ScenarioDefinition)

  /**
    * Notifies about an immediate step exection start
    *
    * @param step steo that is about to be executed
    */
  def stepStarting(step: Step)

  /**
    * Notifies about an error occurred during execution of a step
    *
    * @param step      step that failed
    * @param throwable error that occurred
    */
  def stepFailed(step: Step, throwable: Throwable)

  /**
    * Notifies about a step completion
    *
    * @param step completed step
    */
  def stepCompleted(step: Step)

  /**
    * Notifies about a scenario completion
    *
    * @param scenario completed scenarion definition
    * @param result   result of scenario definition execution
    */
  def scenarioCompleted(scenario: ScenarioDefinition, result: ScenarioResult)

  /**
    * Notifies about a feature completion
    *
    * @param feature completed feature definition
    * @param result  result of the feature definition execution
    */
  def featureCompleted(feature: FeatureDefinition, result: FeatureResult)

  /**
    * Notifies that a path expression matched
    *
    * @param peval path expression evaluation information
    */
  def pathExpressionResult(peval: PathExpressionEvaluation)

}

/**
  * Represents a path expression evaluation in a test
  *
  * @param pathExpression path expression that matched or not
  * @param event          event that fired
  * @param nodes          nodes that matched
  */
case class PathExpressionEvaluation(pathExpression: String, event: GraphNode, nodes: Seq[GraphNode]) {

  /**
    * Did any nodes match?
    */
  def matched: Boolean = nodes.nonEmpty
}

/**
  * Simple no-op GherkinExecutionListener allowing clients to overwrite just certain
  * methods of interest.
  */
class GherkinExecutionListenerAdapter extends GherkinExecutionListener {

  override def featureStarting(feature: FeatureDefinition): Unit = {}

  override def scenarioStarting(scenario: ScenarioDefinition): Unit = {}

  override def stepStarting(step: Step): Unit = {}

  override def stepFailed(step: Step, throwable: Throwable): Unit = {}

  override def stepCompleted(step: Step): Unit = {}

  override def scenarioCompleted(scenario: ScenarioDefinition, result: ScenarioResult): Unit = {}

  override def featureCompleted(feature: FeatureDefinition, result: FeatureResult): Unit = {}

  override def pathExpressionResult(peval: PathExpressionEvaluation): Unit = {}
}
