package com.atomist.rug.test.gherkin

import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.runtime.js.interop.{NashornUtils, jsSafeCommittingProxy}
import com.atomist.source.EmptyArtifactSource
import gherkin.ast.ScenarioDefinition
import jdk.nashorn.api.scripting.ScriptObjectMirror
import scala.collection.JavaConverters._

/**
  * Created by rod on 2/26/17.
  */
case class ExecutableFeature(
                              definition: FeatureDefinition,
                              definitions: Definitions) {

  def execute(): FeatureResult = {
    FeatureResult(definition.feature,
      definition.feature.getChildren.asScala
        .map(executeScenario)
    )
  }

  private def executeScenario(scenario: ScenarioDefinition): ScenarioResult = {
    val project = new ProjectMutableView(EmptyArtifactSource())
    for (step <- scenario.getSteps.asScala) {
      step.getKeyword match {
        case "Given " =>
          val somo = definitions.givenFor(step.getText)
          println(s"Given for [${step.getText}]=$somo")
          somo match {
            case Some(som) =>
              som.callMember("apply", new jsSafeCommittingProxy(project))
            case None => ???
          }
        case "When " =>
          val somo = definitions.whenFor(step.getText)
          println(s"When for [${step.getText}]=$somo")
          somo match {
            case Some(som) =>
              som.callMember("apply", new jsSafeCommittingProxy(project))
            case None => ???
          }
        case "Then " =>
          val somo = definitions.thenFor(step.getText)
          println(s"Then for [${step.getText}]=$somo")
          somo match {
            case Some(som) =>
              val r = som.callMember("apply", new jsSafeCommittingProxy(project))
              r match {
                case rsom: ScriptObjectMirror =>
                  val result = NashornUtils.stringProperty(rsom, "result", "false") == "true"
                  println(s"Raw result=$r, unwrapped = $result")
                case wtf => throw new IllegalArgumentException(s"Unexpected: $wtf")
              }
            case None => ???
          }
      }

    }
    ???
  }

}
