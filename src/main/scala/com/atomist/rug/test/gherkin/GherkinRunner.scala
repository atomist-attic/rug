package com.atomist.rug.test.gherkin

import com.atomist.project.archive.Rugs
import com.atomist.rug.runtime.js.JavaScriptContext
import com.atomist.rug.test.gherkin.project.ProjectManipulationFeature
import com.typesafe.scalalogging.LazyLogging
import gherkin.ast.{ScenarioDefinition, Step}

/**
  * Combine Gherkin DSL BDD definitions with JavaScript backing code
  * and provide the ability to execute the tests
  * @param jsc JavaScript backed by a Rug archive
  * @param rugs other Rugs from the achive
  * @param listeners optional execution listeners
  */
class GherkinRunner(jsc: JavaScriptContext, rugs: Option[Rugs] = None, listeners: Seq[GherkinExecutionListener] = Nil)
  extends LazyLogging {

  import GherkinRunner._

  private val featureFactory: ExecutableFeatureFactory = DefaultExecutableFeatureFactory

  private val definitions = new Definitions(jsc)

  jsc.engine.put(DefinitionsObjectName, definitions)
  jsc.atomistContent
    .filter(_ => true, jsc.atomistConfig.isJsTest)
    .allFiles
    .foreach(f => {
      jsc.evaluate(f)
    })

  /**
    * Features found in this archive
    */
  val features: Seq[FeatureDefinition] = GherkinReader.findFeatures(jsc.rugAs)

  private val executableFeatures = features.map(f =>
    featureFactory.executableFeatureFor(f, definitions, jsc.rugAs, rugs, listeners))

  /**
    * Execute all the tests in this archive that pass provided filter
    * @param filter callback to filter features that should execute
    */
  def execute(filter: (FeatureDefinition) => Boolean = (fd) => true): ArchiveTestResult = {
    logger.info(s"Execute on $this")
    ArchiveTestResult(executableFeatures.filter(pmf => filter.apply(pmf.definition)).map(ef => jsc.withEnhancedExceptions {
      listeners.foreach(_.featureStarting(ef.definition))
      val result = ef.execute()
      listeners.foreach(_.featureCompleted(ef.definition, result))
      result
    }))
  }

  override def toString: String =
    s"${getClass.getSimpleName}: Features [${features}] found in $jsc"

}

object GherkinRunner {

  val DefinitionsObjectName: String = (getClass.getName + "_definitions").replace(".", "_")

}

/**
  * Simple listener trait that can be implemented to receive notifications during
  * test execution.
  */
trait GherkinExecutionListener {

  /**
    * Notifies about an immediate feature definition execution start
    * @param feature feature definition that is about to be executed
    */
  def featureStarting(feature: FeatureDefinition)

  /**
    * Notifies about an immediate scenrio definition execution start
    * @param scenario scenarion definition that is about to be executed
    */
  def scenarioStarting(scenario: ScenarioDefinition)

  /**
    * Notifies about an immediate step exection start
    * @param step steo that is about to be executed
    */
  def stepStarting(step: Step)

  /**
    * Notifies about an error occurred during execution of a step
    * @param step step that failed
    * @param throwable error that occurred
    */
  def stepFailed(step: Step, throwable: Throwable)

  /**
    * Notifies about a step completion
    * @param step completed step
    */
  def stepCompleted(step: Step)

  /**
    * Notifies about a scenario completion
    * @param scenario completed scenarion definition
    * @param result result of scenario definition execution
    */
  def scenarioCompleted(scenario: ScenarioDefinition, result: ScenarioResult)

  /**
    * Notifies about a feature completion
    * @param feature completed feature definition
    * @param result result of the feature definition execution
    */
  def featureCompleted(feature: FeatureDefinition, result: FeatureResult)

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
}
