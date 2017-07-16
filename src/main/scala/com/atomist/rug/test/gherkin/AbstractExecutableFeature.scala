package com.atomist.rug.test.gherkin

import java.lang.reflect.InvocationTargetException

import com.atomist.graph.GraphNode
import com.atomist.project.archive.Rugs
import com.atomist.project.common.InvalidParametersException
import com.atomist.rug.runtime.js.nashorn.jsSafeCommittingProxy
import com.atomist.rug.runtime.js.{JavaScriptObject, UNDEFINED}
import com.eclipsesource.v8.V8ScriptExecutionException
import com.typesafe.scalalogging.LazyLogging
import gherkin.ast.{ScenarioDefinition, Step}

import scala.collection.JavaConverters._

/**
  * Superclass for all features, regardless of what they act on
  */
abstract class AbstractExecutableFeature[W <: ScenarioWorld](
                                                              val definition: FeatureDefinition,
                                                              val definitions: Definitions,
                                                              val rugs: Option[Rugs],
                                                              listeners: Seq[GherkinExecutionListener],
                                                              config: GherkinRunnerConfig)
  extends LazyLogging {

  def execute(): FeatureResult = {
    FeatureResult(definition.feature,
      definition.feature.getChildren.asScala
        .map(executeScenario)
    )
  }

  /**
    * Create a world for overall context
    * This creates a default world.
    */
  protected def createWorldForScenario(): ScenarioWorld

  private def executeScenario(scenario: ScenarioDefinition): ScenarioResult = {
    listeners.foreach(_.scenarioStarting(scenario))
    val world = createWorldForScenario()
    val assertionResults: Seq[AssertionResult] =
      scenario.getSteps.asScala.flatMap(step => {
        listeners.foreach(_.stepStarting(step))
        val result = step.getKeyword match {
          case "Given " if !world.aborted =>
            runGiven(world, step)
          case "When " if !world.aborted =>
            runWhen(world, step)
          case "Then " if step.getText == "the scenario aborted" =>
            Some(AssertionResult(step.getText, Result(world.aborted, "Scenario aborted")))
          case "Then " if !world.aborted =>
            Some(runThen(world, step))
          case "Then " if world.aborted =>
            if(world.abortedCause.nonEmpty){
              world.abortedCause.get.printStackTrace()
            }
            Some(AssertionResult(step.getText,
              Failed(s"Scenario aborted: Could not evaluate: [${world.abortMessage}]", world.abortedCause)))
          case _ =>
            None
        }
        listeners.foreach(_.stepCompleted(step))
        result
      })
    val sr = ScenarioResult(scenario, assertionResults, "")
    listeners.foreach(_.scenarioCompleted(scenario, sr))
    sr
  }

  private def runThen(world: ScenarioWorld, step: Step): AssertionResult = {
    val somo = definitions.thenFor(step.getText)
    logger.debug(s"Then for [${step.getText}]=$somo")
    somo match {
      case Some(stepMatch) =>
        val r = callFunction(stepMatch, world)
        r match {
          case Right(b: java.lang.Boolean) =>
            AssertionResult(step.getText, Result(b, stepMatch.jsVar.toString))
          case Right(rsom: JavaScriptObject) =>
            val result = rsom.stringProperty("result", "false") == "true"
            AssertionResult(step.getText, Result(result, rsom.stringProperty("message", "Detailed information unavailable")))
          case Right(UNDEFINED) =>
            // Returning void (which will be undefined) is truthy
            // This enables use of frameworks such as as chai
            AssertionResult(step.getText, Result(f = true, stepMatch.jsVar.toString))
          case Right(wtf) =>
            throw new IllegalArgumentException(s"Unexpected result from Then '${step.getText}': $wtf")
          case Left(t) =>
            AssertionResult(step.getText, Failed(t.getMessage, Some(t)))
        }
      case None =>
        AssertionResult(step.getText, NotYetImplemented("Then " + step.getText))
    }
  }

  private def runWhen(world: ScenarioWorld, step: Step): Option[AssertionResult] = {
    val somo = definitions.whenFor(step.getText)
    logger.debug(s"When for [${step.getText}]=$somo")
    somo match {
      case Some(som) =>
        callFunction(som, world) match {
          case Left(ipe: InvalidParametersException) =>
            listeners.foreach(_.stepFailed(step, ipe))
            world.logInvalidParameters(ipe)
            None
          case Left(e: Exception) if e.getCause != null && e.getCause.isInstanceOf[InvalidParametersException] =>
            // Can sometimes get this wrapped
            listeners.foreach(_.stepFailed(step, e.getCause.asInstanceOf[InvalidParametersException]))
            world.logInvalidParameters(e.getCause.asInstanceOf[InvalidParametersException])
            None
          case Left(t) =>
            listeners.foreach(_.stepFailed(step, t))
            logger.error(t.getMessage, t)
            world.abort(t)
            None
          case _ =>
            None
        }
      case None =>
        Some(AssertionResult(step.getText, NotYetImplemented("When " + step.getText)))
    }
  }

  private def runGiven(world: ScenarioWorld, step: Step): Option[AssertionResult] = {
    val somo = definitions.givenFor(step.getText)
    logger.debug(s"Given for [${step.getText}]=$somo")
    somo match {
      case Some(som) =>
        callFunction(som, world) match {
          case Left(t) =>
            world.abort(t)
            listeners.foreach(_.stepFailed(step, t))
            logger.error(t.getMessage, t)
            None
          case _ =>
            None
        }
      case None =>
        Some(AssertionResult(step.getText, NotYetImplemented("Given " + step.getText)))
    }
  }

  // Call a JavaScriptObject function with appropriate error handling
  private def callFunction(sm: StepMatch, world: ScenarioWorld): Either[Throwable, Object] = {
    val target = world.target
    // Only include the target if it's different from the world.
    val fixedParams: Seq[AnyRef] = target match {
      case `world` => Seq(world)
      case _ => Seq(target, world)
    }
    val args = fixedParams ++ sm.args

    try{
      Right(sm.jsVar.call("apply", args:_*))
    }catch {
      // TODO really needs to be in the particular JS engine
      case v: V8ScriptExecutionException => v.getCause match {
        case i: InvocationTargetException => Left(i.getTargetException)
        case _ => Left(v)
      }
      case x: Throwable => Left(x)
    }
  }

}
