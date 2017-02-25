package com.atomist.rug.test.gherkin

import com.atomist.rug.runtime.js.JavaScriptContext
import gherkin.ast.{Feature, Scenario}
import jdk.nashorn.api.scripting.ScriptObjectMirror

sealed trait Result

case object Passed extends Result

case object Failed extends Result

case object NotYetImplemented extends Result

case class ScenarioResult(scen: Scenario, result: Result, data: String)

case class FeatureResult(f: Feature, scenarioResults: Seq[ScenarioResult]) {

  def passed: Boolean = scenarioResults.forall(_.result == Passed)
}

case class TestResult(featureResults: Seq[FeatureResult]) {

  def passed: Boolean = featureResults.forall(_.passed)
}


class GherkinRunner(jsc: JavaScriptContext) {

  import GherkinRunner._

  private val definitions = new Definitions()

  jsc.engine.put(DefinitionsObjectName, definitions)
  //jsc.engine.eval(SetupJs)
  jsc.atomistContent
    .filter(_ => true, jsc.atomistConfig.isJsTest)
    .allFiles
    .foreach(jsc.evaluate)

  val features: Seq[FeatureDefinition] = GherkinReader.findFeatures(jsc.rugAs)

  private val executableFeatures = features.map(f => ExecutableFeature(f, definitions))

  def execute(): TestResult = {
    TestResult(executableFeatures.map(ef => ef.execute()))
  }

}

object GherkinRunner {

  val DefinitionsObjectName = "_definitions"
}


private[gherkin] class Definitions {

  private val stepRegistry = new scala.collection.mutable.HashMap[String, ScriptObjectMirror]()

  def Given(s: String, what: ScriptObjectMirror): Unit = {
    println(s"Registering Given for [$s]")
    stepRegistry.put("given_" + s, what)
  }

  def When(s: String, what: ScriptObjectMirror): Unit = {
    println(s"Registering When for [$s]")
    stepRegistry.put("when_" + s, what)
  }

  def Then(s: String, what: ScriptObjectMirror): Unit = {
    println(s"Registering Then for [$s]")
    stepRegistry.put("then_" + s, what)
  }

  def whenFor(s: String): Option[ScriptObjectMirror] = {
    println(s"Query for [$s]")
    stepRegistry.get("when_" + s)
  }

  def givenFor(s: String): Option[ScriptObjectMirror] = stepRegistry.get("given_" + s)

  def thenFor(s: String): Option[ScriptObjectMirror] = stepRegistry.get("then_" + s)


}


