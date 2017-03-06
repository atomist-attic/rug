package com.atomist.rug.test.gherkin

import com.atomist.rug.runtime.js.JavaScriptContext
import com.atomist.source.ArtifactSource
import com.typesafe.scalalogging.LazyLogging
import jdk.nashorn.api.scripting.ScriptObjectMirror

/**
  * Definitions of JavaScript function in a JavaScriptContext
  */
class Definitions(
                   val jsc: JavaScriptContext)
  extends LazyLogging {

  private val stepRegistry = new scala.collection.mutable.HashMap[String, ScriptObjectMirror]()

  def rugArchive: ArtifactSource = jsc.rugAs

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
