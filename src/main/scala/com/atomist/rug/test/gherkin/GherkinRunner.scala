package com.atomist.rug.test.gherkin

import com.atomist.rug.runtime.js.JavaScriptContext
import com.typesafe.scalalogging.LazyLogging
import jdk.nashorn.api.scripting.ScriptObjectMirror

class GherkinRunner(jsc: JavaScriptContext) {

  import GherkinRunner._

  private val definitions = new Definitions()

  jsc.engine.put(DefinitionsObjectName, definitions)
  jsc.atomistContent
    .filter(_ => true, jsc.atomistConfig.isJsTest)
    .allFiles
    .foreach(jsc.evaluate)

  val features: Seq[FeatureDefinition] = GherkinReader.findFeatures(jsc.rugAs)

  private val executableFeatures = features.map(f => new ProjectManipulationFeature(f, definitions))

  def execute(): TestResult = {
    TestResult(executableFeatures.map(ef => jsc.withEnhancedExceptions {
      ef.execute()
    }))
  }

}

object GherkinRunner {

  val DefinitionsObjectName = "_definitions"
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


