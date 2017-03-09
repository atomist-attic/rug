package com.atomist.rug.test.gherkin

import com.atomist.graph.GraphNode
import com.atomist.project.common.InvalidParametersException
import com.atomist.rug.runtime.js.interop.{NashornUtils, jsSafeCommittingProxy}
import com.typesafe.scalalogging.LazyLogging
import gherkin.ast.{ScenarioDefinition, Step}
import jdk.nashorn.api.scripting.ScriptObjectMirror

import scala.collection.JavaConverters._

/**
  * Superclass for all features, regardless of what they act on
  */
abstract class AbstractExecutableFeature[T <: Object, W <: ScenarioWorld](
                                          val definition: FeatureDefinition,
                                          val definitions: Definitions,
                                          listeners: Seq[GherkinExecutionListener] = Nil)
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
  protected def createWorldForScenario(target: T): ScenarioWorld

  private def executeScenario(scenario: ScenarioDefinition): ScenarioResult = {
    listeners.foreach(_.scenarioStarting(scenario))
    val fixture = createFixture
    val world = createWorldForScenario(fixture)
    val assertionResults: Seq[AssertionResult] =
      scenario.getSteps.asScala.flatMap(step => {
        step.getKeyword match {
          case "Given " if !world.aborted =>
            runGiven(fixture, world, step)
            None
          case "When " if !world.aborted =>
            runWhen(fixture, world, step)
            None
          case "Then " if !world.aborted =>
            Some(runThen(fixture, world, step))
          case "Then " if world.aborted =>
            Some(AssertionResult(step.getText, Failed("Scenario aborted: Could not evaluate")))
          case _ =>
            None
        }
      })
    val sr = ScenarioResult(scenario, assertionResults, "")
    listeners.foreach(_.scenarioCompleted(scenario, sr))
    sr
  }

  private def runThen(target: T, world: ScenarioWorld, step: Step): AssertionResult = {
    val somo = definitions.thenFor(step.getText)
    logger.debug(s"Then for [${step.getText}]=$somo")
    somo match {
      case Some(som) =>
        val r = callFunction(som, target, world)
        r match {
          case Right(b: java.lang.Boolean) =>
            AssertionResult(step.getText, Result(b, som.toString))
          case Right(rsom: ScriptObjectMirror) =>
            val result = NashornUtils.stringProperty(rsom, "result", "false") == "true"
            //println(s"Raw result=$r, unwrapped = $result")
            AssertionResult(step.getText, Result(result, NashornUtils.stringProperty(rsom, "message", "Detailed information unavailable")))
          case Right(wtf) =>
            throw new IllegalArgumentException(s"Unexpected result from Then '${step.getText}': $wtf")
          case Left(t) =>
            AssertionResult(step.getText, Failed(t.getMessage))
        }
      case None =>
        //println(s"No Then for [${step.getText}]=$somo")
        AssertionResult(step.getText, NotYetImplemented(step.getText))
    }
  }

  private def runWhen(target: T, world: ScenarioWorld, step: Step): Unit = {
    val somo = definitions.whenFor(step.getText)
    logger.debug(s"When for [${step.getText}]=$somo")
    somo match {
      case Some(som) =>
        callFunction(som, target, world) match {
          case Left(ipe: InvalidParametersException) =>
            world.logInvalidParameters(ipe)
          case Left(e: Exception) if e.getCause != null && e.getCause.isInstanceOf[InvalidParametersException] =>
            // Can sometimes get this wrapped
            world.logInvalidParameters(e.getCause.asInstanceOf[InvalidParametersException])
          case Left(t) =>
            println(s"\t\t${t.getMessage}")
            logger.error(t.getMessage, t)
            world.abort()
          case _ =>
            // Do nothing
        }
      case None =>
        logger.info(s"When [${step.getText}] not yet implemented")
    }
  }

  private def runGiven(target: T, world: ScenarioWorld, step: Step):Unit = {
    val somo = definitions.givenFor(step.getText)
    logger.debug(s"Given for [${step.getText}]=$somo")
    somo match {
      case Some(som) =>
        callFunction(som, target, world) match {
          case Left(t) =>
            println(s"\t\t${t.getMessage}")
            logger.error(t.getMessage, t)
            world.abort()
            case _ =>
            // OK
        }
      case None =>
        logger.info(s"Given [${step.getText}] not yet implemented")
    }
  }

  // Call a ScriptObjectMirror function with appropriate error handling
  private def callFunction(som: ScriptObjectMirror, target: T, world: Object): Either[Throwable,Object] = {
    import scala.util.control.Exception._
    allCatch.either(som.call("apply",
      target match {
        case gn: GraphNode => new jsSafeCommittingProxy(gn)
        case _ => target
      },
      world)
    )
  }

}
