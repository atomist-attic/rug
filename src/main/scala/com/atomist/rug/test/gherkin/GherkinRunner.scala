package com.atomist.rug.test.gherkin

import com.atomist.project.archive.Rugs
import com.atomist.rug.runtime.js.{JavaScriptContext, JavaScriptEngine}
import com.typesafe.scalalogging.LazyLogging

/**
  * Config to use when running tests. Tokens etc.
  *
  * @param oAuthToken token to use for Git cloning
  */
case class GherkinRunnerConfig(oAuthToken: Option[String] = None)

/**
  * Combine Gherkin DSL BDD definitions with JavaScript backing code
  * and provide the ability to execute the tests
  *
  * @param jsc       JavaScript backed by a Rug archive
  * @param rugs      other Rugs from the archive
  * @param listeners optional execution listeners
  * @param config    config to use for test running
  */
class GherkinRunner(jsc: JavaScriptContext,
                    rugs: Option[Rugs] = None,
                    listeners: Seq[GherkinExecutionListener] = Nil,
                    config: GherkinRunnerConfig = GherkinRunnerConfig())
  extends LazyLogging {

  import GherkinRunner._

  private val featureFactory: ExecutableFeatureFactory = DefaultExecutableFeatureFactory

  private val definitions = new Definitions(jsc)

  jsc.setMember(DefinitionsObjectName, definitions)
  jsc.atomistContent()
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
    featureFactory.executableFeatureFor(f, definitions, jsc.rugAs, rugs, listeners, config))

  /**
    * Execute all the tests in this archive that pass provided filter
    *
    * @param filter callback to filter features that should execute
    */
  def execute(filter: (FeatureDefinition) => Boolean = _ => true): ArchiveTestResult = {
    logger.info(s"Execute on $this")
    ArchiveTestResult(executableFeatures.filter(pmf => filter(pmf.definition)).map(ef => {
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
