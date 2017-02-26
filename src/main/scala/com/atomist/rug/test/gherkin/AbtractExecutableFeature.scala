package com.atomist.rug.test.gherkin

import com.atomist.graph.GraphNode
import com.atomist.rug.runtime.js.interop.{NashornUtils, jsSafeCommittingProxy}
import com.typesafe.scalalogging.LazyLogging
import gherkin.ast.{ScenarioDefinition, Step}
import jdk.nashorn.api.scripting.ScriptObjectMirror

import scala.collection.JavaConverters._

/**
  * Superclass for all features, regardless of what they act on
  */
abstract class AbstractExecutableFeature[T <: GraphNode](
                                          val definition: FeatureDefinition,
                                          val definitions: Definitions)
  extends LazyLogging {

  def execute(): FeatureResult = {
    FeatureResult(definition.feature,
      definition.feature.getChildren.asScala
        .map(executeScenario)
    )
  }

  protected def createTarget: T

  // TODO world concept

  private def executeScenario(scenario: ScenarioDefinition): ScenarioResult = {
    val project = createTarget
    val assertionResults: Seq[AssertionResult] =
      scenario.getSteps.asScala.flatMap(step => {
        step.getKeyword match {
          case "Given " =>
            runGiven(project, step)
            None
          case "When " =>
            runWhen(project, step)
            None
          case "Then " =>
            Some(runThen(project, step))
        }
      })
    val sr = ScenarioResult(scenario, assertionResults, "")
    //println(sr)
    sr
  }

  private def runThen(target: T, step: Step): AssertionResult = {
    val somo = definitions.thenFor(step.getText)
    logger.debug(s"Then for [${step.getText}]=$somo")
    somo match {
      case Some(som) =>
        // TODO #187. We might be interested in a reviewer, in which case we should be able to get at a review context.
        // Could look for review in the text?
        val r = som.call("apply", new jsSafeCommittingProxy(target))
        r match {
          case b: java.lang.Boolean =>
            AssertionResult(step.getText, Result(b, som.toString))
          case rsom: ScriptObjectMirror =>
            val result = NashornUtils.stringProperty(rsom, "result", "false") == "true"
            //println(s"Raw result=$r, unwrapped = $result")
            AssertionResult(step.getText, Result(result, NashornUtils.stringProperty(rsom, "message", "Detailed information unavailable")))
          case wtf => throw new IllegalArgumentException(s"Unexpected: $wtf")
        }
      case None =>
        println(s"No Then for [${step.getText}]=$somo")
        AssertionResult(step.getText, NotYetImplemented)
    }
  }

  private def runWhen(target: T, step: Step) = {
    val somo = definitions.whenFor(step.getText)
    logger.debug(s"When for [${step.getText}]=$somo")
    somo match {
      case Some(som) =>
        som.call("apply", new jsSafeCommittingProxy(target))
      case None =>
        println(s"Warning: When [${step.getText}] not yet implemented")
    }
  }

  private def runGiven(target: T, step: Step) = {
    val somo = definitions.givenFor(step.getText)
    logger.debug(s"Given for [${step.getText}]=$somo")
    somo match {
      case Some(som) =>
        som.call("apply", new jsSafeCommittingProxy(target))
      case None =>
        println(s"Warning: Given [${step.getText}] not yet implemented")
    }
  }
}
