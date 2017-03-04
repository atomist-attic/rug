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

  protected def createFixture: T

  /**
    * Create a world for overall context, based on the fixture.
    * This creates a default world.
    */
  protected def createWorld(target: T): World = new World(definitions)

  private def executeScenario(scenario: ScenarioDefinition): ScenarioResult = {
    println(s"\tExecuting test scenario ${scenario.getName}")
    val fixture = createFixture
    val world = createWorld(fixture)
    val assertionResults: Seq[AssertionResult] =
      scenario.getSteps.asScala.flatMap(step => {
        step.getKeyword match {
          case "Given " =>
            runGiven(fixture, world, step)
            None
          case "When " =>
            runWhen(fixture, world, step)
            None
          case "Then " =>
            Some(runThen(fixture, world, step))
        }
      })
    val sr = ScenarioResult(scenario, assertionResults, "")
    //println(sr)
    sr
  }

  private def runThen(target: T, world: Object, step: Step): AssertionResult = {
    val somo = definitions.thenFor(step.getText)
    logger.debug(s"Then for [${step.getText}]=$somo")
    somo match {
      case Some(som) =>
        // TODO #187. We might be interested in a reviewer, in which case we should be able to get at a review context.
        // Could look for review in the text?
        val r = callFunction(som, target, world)
        r match {
          case Right(b: java.lang.Boolean) =>
            AssertionResult(step.getText, Result(b, som.toString))
          case Right(rsom: ScriptObjectMirror) =>
            val result = NashornUtils.stringProperty(rsom, "result", "false") == "true"
            //println(s"Raw result=$r, unwrapped = $result")
            AssertionResult(step.getText, Result(result, NashornUtils.stringProperty(rsom, "message", "Detailed information unavailable")))
          case Right(wtf) =>
            throw new IllegalArgumentException(s"Unexpected: $wtf")
          case Left(t) =>
            AssertionResult(step.getText, Failed(t.getMessage))
        }
      case None =>
        //println(s"No Then for [${step.getText}]=$somo")
        AssertionResult(step.getText, NotYetImplemented(step.getText))
    }
  }

  private def runWhen(target: T, world: Object, step: Step) = {
    val somo = definitions.whenFor(step.getText)
    logger.debug(s"When for [${step.getText}]=$somo")
    somo match {
      case Some(som) =>
        callFunction(som, target, world)
      case None =>
        logger.info(s"When [${step.getText}] not yet implemented")
    }
  }

  private def runGiven(target: T, world: Object, step: Step) = {
    val somo = definitions.givenFor(step.getText)
    logger.debug(s"Given for [${step.getText}]=$somo")
    somo match {
      case Some(som) =>
        callFunction(som, target, world)
      case None =>
        logger.info(s"Given [${step.getText}] not yet implemented")
    }
  }

  // Call a ScriptObjectMirror function with appropriate error handling
  private def callFunction(som: ScriptObjectMirror, target: T, world: Object): Either[Throwable,Object] = {
    import scala.util.control.Exception._
    allCatch.either(som.call("apply", new jsSafeCommittingProxy(target), world))
  }

}
