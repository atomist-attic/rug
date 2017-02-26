package com.atomist.rug.test.gherkin

import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.runtime.js.interop.{NashornUtils, jsSafeCommittingProxy}
import com.atomist.source.EmptyArtifactSource
import com.typesafe.scalalogging.LazyLogging
import gherkin.ast.{ScenarioDefinition, Step}
import jdk.nashorn.api.scripting.ScriptObjectMirror

import scala.collection.JavaConverters._

private[gherkin] case class ExecutableFeature(
                                               definition: FeatureDefinition,
                                               definitions: Definitions)
  extends LazyLogging {

  def execute(): FeatureResult = {
    FeatureResult(definition.feature,
      definition.feature.getChildren.asScala
        .map(executeScenario)
    )
  }

  private def executeScenario(scenario: ScenarioDefinition): ScenarioResult = {
    val project = new ProjectMutableView(EmptyArtifactSource())
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
    ScenarioResult(scenario, assertionResults, "")
  }

  private def runThen(project: ProjectMutableView, step: Step): AssertionResult = {
    val somo = definitions.thenFor(step.getText)
    logger.debug(s"Then for [${step.getText}]=$somo")
    somo match {
      case Some(som) =>
        val r = som.callMember("apply", new jsSafeCommittingProxy(project))
        r match {
          case rsom: ScriptObjectMirror =>
            val result = NashornUtils.stringProperty(rsom, "result", "false") == "true"
            //println(s"Raw result=$r, unwrapped = $result")
            if (result)
              AssertionResult(step.getText, Passed)
            else
              AssertionResult(step.getText, Failed(NashornUtils.stringProperty(rsom, "message", "Detailed information unavailable")))
          case wtf => throw new IllegalArgumentException(s"Unexpected: $wtf")
        }
      case None =>
        AssertionResult(step.getText, NotYetImplemented)
    }
  }

  private def runWhen(project: ProjectMutableView, step: Step) = {
    val somo = definitions.whenFor(step.getText)
    logger.debug(s"When for [${step.getText}]=$somo")
    somo match {
      case Some(som) =>
        som.callMember("apply", new jsSafeCommittingProxy(project))
      case None =>
        println(s"Warning: When [${step.getText}] not yet implemented")
    }
  }

  private def runGiven(project: ProjectMutableView, step: Step) = {
    val somo = definitions.givenFor(step.getText)
    logger.debug(s"Given for [${step.getText}]=$somo")
    somo match {
      case Some(som) =>
        som.callMember("apply", new jsSafeCommittingProxy(project))
      case None =>
        println(s"Warning: Given [${step.getText}] not yet implemented")
    }
  }
}
