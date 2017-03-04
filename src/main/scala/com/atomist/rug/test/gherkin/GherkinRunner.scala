package com.atomist.rug.test.gherkin

import com.atomist.rug.runtime.js.JavaScriptContext
import com.atomist.util.lang.JavaHelpers
import com.typesafe.scalalogging.LazyLogging
import jdk.nashorn.api.scripting.ScriptObjectMirror

/**
  * Combine Gherkin DSL BDD definitions with JavaScript backing code
  * and provide the ability to execute the tests
  * @param jsc JavaScript backed by a Rug archive
  */
class GherkinRunner(jsc: JavaScriptContext) extends LazyLogging {

  import GherkinRunner._

  private val definitions = new Definitions()

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

  private val executableFeatures = features.map(f => new ProjectManipulationFeature(f, definitions))

  /**
    * Execute all the tests in this archive
    */
  def execute(): ArchiveTestResult = {
    logger.info(s"Execute on $this")
    ArchiveTestResult(executableFeatures.map(ef => jsc.withEnhancedExceptions {
      println(s"Executing feature ${ef.definition.feature.getName}")
      ef.execute()
    }))
  }

  override def toString: String =
    s"${getClass.getSimpleName}: Features [${features}] found in $jsc"

}

object GherkinRunner {

  val DefinitionsObjectName: String = (getClass.getName + "_definitions").replace(".", "_")

}


private[gherkin] class Definitions extends LazyLogging {

  private val stepRegistry = new scala.collection.mutable.HashMap[String, ScriptObjectMirror]()

  def Given(s: String, what: ScriptObjectMirror): Unit = {
    logger.debug(s"Registering Given for [$s]")
    stepRegistry.put("given_" + s, what)
  }

  def When(s: String, what: ScriptObjectMirror): Unit = {
    logger.debug(s"Registering When for [$s]")
    stepRegistry.put("when_" + s, what)
  }

  def Then(s: String, what: ScriptObjectMirror): Unit = {
    logger.debug(s"Registering Then for [$s]")
    stepRegistry.put("then_" + s, what)
  }

  def whenFor(s: String): Option[ScriptObjectMirror] = stepRegistry.get("when_" + s)

  def givenFor(s: String): Option[ScriptObjectMirror] = stepRegistry.get("given_" + s)

  def thenFor(s: String): Option[ScriptObjectMirror] = stepRegistry.get("then_" + s)

}


