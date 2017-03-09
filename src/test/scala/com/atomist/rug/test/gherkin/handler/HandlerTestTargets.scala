package com.atomist.rug.test.gherkin.handler

import com.atomist.source.StringFileArtifact

object HandlerTestTargets {

  val Feature1 =
    """
      |Feature: Australian political history
      | This is a test
      | to demonstrate that the Gherkin DSL
      | is a good fit for Rug BDD testing
      |
      |Scenario: Australian politics, 1972-1991
      | Given a sleepy country
      | When a visionary leader enters
      | Then excitement ensues
    """.stripMargin

  val Feature1File = StringFileArtifact(
    ".atomist/test/handler/Feature1.feature",
    Feature1
  )

  val PassingFeature1Steps =
    """
      |import {Project} from "@atomist/rug/model/Core"
      |import {ProjectEditor} from "@atomist/rug/operations/ProjectEditor"
      |import {Given,When,Then} from "@atomist/rug/test/project/Core"
      |
      |Given("a sleepy country", f => {
      |
      |})
      |When("a visionary leader enters", f => {
      |
      |})
      |Then("excitement ensues", p => true)
    """.stripMargin

  val PassingFeature1StepsFile = StringFileArtifact(
    ".atomist/test/handler/PassingFeature1Step.ts",
    PassingFeature1Steps
  )
}
