package com.atomist.rug.test.gherkin.project

import com.atomist.rug.TestUtils
import com.atomist.source.{FileArtifact, StringFileArtifact}

object ProjectTestTargets {

  val PassingSimpleTsFile: FileArtifact =
    TestUtils.requiredFileInPackage(this, "PassingSimple.ts").withPath(".atomist/test/project/Simple_definitions.ts")

  val FailingSimpleTsFile: FileArtifact =
    TestUtils.requiredFileInPackage(this, "FailingSimple.ts").withPath(".atomist/test/project/Simple_definitions.ts")

  val EditorWithoutParametersTsFile = StringFileArtifact(".atomist/test/project/Simple_definitions.ts",
    TestUtils.requiredFileInPackage(this, "EditorWithoutParametersSteps.ts").content)

  val EditorWithParametersStepsFile = StringFileArtifact(".atomist/test/project/Simple_definitions.ts",
    TestUtils.requiredFileInPackage(this, "EditorWithParametersSteps.ts").content)

  val EditorWithBadParametersStepsFile = StringFileArtifact(".atomist/test/project/Simple_definitions.ts",
    TestUtils.requiredFileInPackage(this, "EditorWithBadParametersSteps.ts").content)

  val CorruptionFeature =
    """
      |Feature: Look for corrupt politicians
      |  This is a test
      |  to see whether
      |  we can test project reviewers
      |
      |  Scenario: Look for convicted criminals
      |    Given a number of files
      |    When run corruption reviewer
      |    Then we have comments
      |""".stripMargin

  val CorruptionFeatureFile = StringFileArtifact(
    ".atomist/tests/project/Corruption.feature",
    CorruptionFeature)

  val GenerationFeature =
    """
      |Feature: Generate a new project
      |  This is a test
      |  to see whether
      |  we can test project generators
      |
      |  Scenario: New project should have content from template
      |    Given an empty project
      |    When run simple generator
      |    Then parameters were valid
      |    Then we have Anders
      |    Then file at src/from/typescript should exist
      |    Then file at src/from/typescript should contain Anders
      |    Then we have file from start project
      |    Then the project name is correct
      |    Then we can succeed by returning void
      |""".stripMargin

  val GenerationFeatureFile = StringFileArtifact(
    ".atomist/tests/project/Generation.feature",
    GenerationFeature)

  /**
    * @param params map to string representation of param, e.g. including "
    */
  def generationTest(gen: String, projectName: String, params: Map[String,String]): String =
    s"""
       |import { Project } from "@atomist/rug/model/Core"
       |import { ProjectGenerator } from "@atomist/rug/operations/ProjectGenerator"
       |import { Given, When, Then, ProjectScenarioWorld } from "@atomist/rug/test/project/Core"
       |
       |When("run simple generator", (p, w) => {
       |    let world = w as ProjectScenarioWorld;
       |    let g = world.generator("$gen");
       |    world.generateWith(g, "$projectName", {${params.map(p => s"${p._1}: ${p._2}").mkString(", ")}});
       |});
       |Then("parameters were valid", (p, world) => !world.invalidParameters())
       |Then("we have Anders", p => {
       |    let f = p.findFile("src/from/typescript");
       |    return f != null && f.content.indexOf("Anders") > -1;
       |});
       |Then("we have file from start project", p => {
       |    return p.findFile("pom.xml") != null;
       |});
       |Then("the project name is correct", p => {
       |    return p.name == "$projectName";
       |});
       |Then("we can succeed by returning void", p => {
       |});
       |""".stripMargin

  val GenerationBadParameterFeature =
    """
      |Feature: Generate a new project
      |  This is a test
      |  to see whether
      |  we can test project generators
      |
      |  Scenario: New project should have content from template
      |    Given an empty project
      |    When run simple generator
      |    Then parameters were invalid
      |""".stripMargin

  val GenerationBadParameterFeatureFile = StringFileArtifact(
    ".atomist/tests/project/GenerationBadParam.feature",
    GenerationBadParameterFeature
  )

  def generateWithInvalidParameters(gen: String, projectName: String, params: Map[String,String]): String =
    s"""
       |import { Project } from "@atomist/rug/model/Core"
       |import { ProjectGenerator } from "@atomist/rug/operations/ProjectGenerator"
       |import { Given, When, Then, ProjectScenarioWorld } from "@atomist/rug/test/project/Core"
       |
       |When("run simple generator", (p, w) => {
       |    let world = w as ProjectScenarioWorld;
       |    let g = world.generator("$gen");
       |    world.generateWith(g, "$projectName", {${params.map(p => s"${p._1}: ${p._2}").mkString(", ")}});
       |});
       |""".stripMargin

  val EditorBadParameterFeature =
    """
      |Feature: Australian political history
      |  This is a test
      |  to demonstrate that the Gherkin DSL
      |  is a good fit for Rug BDD testing
      |
      |  Scenario: Australian politics, 1972-1991
      |    Given an empty project
      |    Given a visionary leader
      |    When politics takes its course
      |    Then parameters were invalid
      |    Then the rage has a name
    """.stripMargin

  val EditorBadParameterFeatureFile =
    StringFileArtifact(".atomist/tests/project/BadParameter.feature", EditorBadParameterFeature)

  val ParameterizedFeature =
    """
      |Feature: Australian political history
      |  This is a test
      |  to demonstrate that the Gherkin DSL
      |  is a good fit for Rug BDD testing
      |
      |  Scenario: Australian politics, 1972-1991
      |    Given a file named Gough
      |    When do nothing
      |    Then the project has 1 files
      |    Then the project has a file named Gough
    """.stripMargin

  val ParameterizedFeatureFile =
    StringFileArtifact(".atomist/tests/project/Parameterized.feature", ParameterizedFeature)

}
