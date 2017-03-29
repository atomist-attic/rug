package com.atomist.rug.test.gherkin

import com.atomist.graph.GraphNode
import com.atomist.project.archive.Rugs
import com.atomist.rug.runtime.js.JavaScriptContext
import com.atomist.rug.test.gherkin.project.ProjectManipulationFeature
import com.typesafe.scalalogging.LazyLogging
import gherkin.ast.{ScenarioDefinition, Step}

/**
  * Combine Gherkin DSL BDD definitions with JavaScript backing code
  * and provide the ability to execute the tests
  *
  * @param jsc       JavaScript backed by a Rug archive
  * @param rugs      other Rugs from the achive
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
    *
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
    s"${getClass.getSimpleName}: Features [$features] found in $jsc"

}

object GherkinRunner {

  val DefinitionsObjectName: String = (getClass.getName + "_definitions").replace(".", "_")

}
