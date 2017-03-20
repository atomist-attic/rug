package com.atomist.rug.test.gherkin

import com.atomist.rug.runtime.js.JavaScriptContext
import com.atomist.source.ArtifactSource
import com.typesafe.scalalogging.LazyLogging
import jdk.nashorn.api.scripting.ScriptObjectMirror

import scala.util.matching.Regex.Match

/**
  * Definitions of JavaScript functions in a JavaScriptContext
  * corresponding to Gherkin scenario step definitions.
  * Parses arguments using regexs.
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

  def whenFor(s: String): Option[StepMatch] = findMatch("when_" + s)

  def givenFor(s: String): Option[StepMatch] = findMatch("given_" + s)

  def thenFor(s: String): Option[StepMatch] = findMatch("then_" + s)

  private def findMatch(s: String): Option[StepMatch] = {
    // Find a key that matches the regex, then extract the capture groups
    stepRegistry.keys.flatMap(key => {
      val r = s.matches(key)
      if (r) {
        val om: Match = key.r.findPrefixMatchOf(s).get
        val args: Seq[AnyRef] = om.subgroups
        Some(StepMatch(stepRegistry(key), args))
      }
      else None
    }).headOption
  }

}

/**
  * Represents a step and the arguments, if any, to it,
  * from processing the regular expression
  * @param jsVar JS var to invoke
  * @param args arguments to pass
  */
case class StepMatch(jsVar: ScriptObjectMirror, args: Seq[AnyRef])
