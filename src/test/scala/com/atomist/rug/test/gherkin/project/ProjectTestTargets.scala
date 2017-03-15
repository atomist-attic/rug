package com.atomist.rug.test.gherkin.project

import com.atomist.source.StringFileArtifact

object ProjectTestTargets {

  val FailingSimpleTs =
    """
      |import {Project} from "@atomist/rug/model/Core"
      |import {ProjectEditor} from "@atomist/rug/operations/ProjectEditor"
      |import {Given,When,Then} from "@atomist/rug/test/project/Core"
      |
      |Given("a visionary leader", p => {
      | p.addFile("Gough", "Maintain the rage")
      |})
      |When("politics takes its course", p => {
      | p.addFile("Malcolm", "Life wasn't meant to be easy")
      | p.deleteFile("Gough")
      |})
      |Then("the rage is maintained", p => p.fileExists("Gough"))
    """.stripMargin

  val PassingSimpleTs =
    s"""
       |import {Project} from "@atomist/rug/model/Core"
       |import {ProjectEditor} from "@atomist/rug/operations/ProjectEditor"
       |import {Given,When,Then} from "@atomist/rug/test/project/Core"
       |
      |Given("a visionary leader", p => {
       | p.addFile("Gough", "Maintain the rage")
       |})
       |When("politics takes its course", (p, world) => {
       | //console.log(`The world is $${world}`)
       |})
       |Then("changes were made", p => true) // Override this one for this test
       |Then("one edit was made", p => true)
       |Then("the rage is maintained", p => p.fileExists("Gough"))
    """.stripMargin

  val EditorWithoutParametersTs =
    """
      |import {Project} from "@atomist/rug/model/Core"
      |import {ProjectEditor} from "@atomist/rug/operations/ProjectEditor"
      |import {Given,When,Then,ProjectScenarioWorld} from "@atomist/rug/test/project/Core"
      |
      |import {AlpEditor} from "../../editors/AlpEditor"
      |
      |Given("a visionary leader", p => {
      | p.addFile("Gough", "Maintain the rage")
      |})
      |When("politics takes its course", (p, w) => {
      |  let world = w as ProjectScenarioWorld
      |  world.editWith(world.editor("AlpEditor"), {})
      |})
      |Then("one edit was made", (p, world) => {
      | console.log(`Editors run=$${world.editorsRun()}`)
      | return world.editorsRun() == 1
      |})
      |Then("the rage is maintained", p => {
      |   return p.fileExists("Paul")
      |})
    """.stripMargin

  val EditorWithParametersTs =
    """
      |import {Project} from "@atomist/rug/model/Core"
      |import {ProjectEditor} from "@atomist/rug/operations/ProjectEditor"
      |import {Given,When,Then,ProjectScenarioWorld} from "@atomist/rug/test/project/Core"
      |
      |import {AlpEditor} from "../../editors/AlpEditor"
      |
      |Given("a visionary leader", p => {
      | p.addFile("Gough", "Maintain the rage")
      |})
      |When("politics takes its course", (p, w) => {
      |  let world = w as ProjectScenarioWorld
      |  world.editWith(world.editor("AlpEditor"), {heir: "Paul"})
      |})
      |Then("one edit was made", (p, world) => {
      | console.log(`Editors run=$${world.editorsRun()}`)
      | return world.editorsRun() == 1
      |})
      |Then("the rage is maintained", p => {
      |   return p.fileExists("Paul")
      |})
    """.stripMargin


  val PassingSimpleTsFile = StringFileArtifact(".atomist/test/project/Simple_definitions.ts",
    PassingSimpleTs)

  val FailingSimpleTsFile = StringFileArtifact(".atomist/test/project/Simple_definitions.ts",
    FailingSimpleTs)

  val EditorWithoutParametersTsFile = StringFileArtifact(".atomist/test/project/Simple_definitions.ts",
    EditorWithoutParametersTs)

  val EditorWithParametersTsFile = StringFileArtifact(".atomist/test/project/Simple_definitions.ts", EditorWithParametersTs)

  val CorruptionFeature =
    """
      |Feature: Look for corrupt politicians
      | This is a test
      | to see whether
      | we can test project reviewers
      |
      |Scenario: Look for convicted criminals
      | Given a number of files
      | When run corruption reviewer
      | Then we have comments
    """.stripMargin

  val CorruptionFeatureFile = StringFileArtifact(
    ".atomist/tests/project/Corruption.feature",
    CorruptionFeature)

  val CorruptionTest =
    """
      |import {Project} from "@atomist/rug/model/Core"
      |import {ProjectEditor} from "@atomist/rug/operations/ProjectEditor"
      |import {Given,When,Then} from "@atomist/rug/test/project/Core"
      |import * as helpers from "@atomist/rug/test/project/Helpers"
      |
      |import {FindCorruption} from "../../editors/FindCorruption"
      |
      |Given("a number of files", p => {
      | p.addFile("NSW", "Wran")
      | p.addFile("Victoria", "Cain")
      | p.addFile("WA", "Brian Burke and WA Inc")
      |})
      |When("run corruption reviewer", (p, world) => {
      |  let r = new FindCorruption()
      |  let rr = r.review(p)
      |   world.put("review", rr)
      |})
      |Then("we have comments", (p, world) => {
      |   if (helpers.prettyListFiles(p).indexOf("NSW") == -1) throw new Error("Bad pretty list")
      |   if (helpers.dump(p, "NSW").indexOf("Wran") == -1)  throw new Error("Bad dump")
      |   let rr = world.get("review")
      |   return rr.comments.length == 1
      |})
    """.stripMargin

  val GenerationFeature =
    """
      |Feature: Generate a new project
      | This is a test
      | to see whether
      | we can test project generators
      |
      |Scenario: New project should have content from template
      | Given an empty project
      | When run simple generator
      | Then parameters were valid
      | Then we have Anders
      | Then we have file from start project
    """.stripMargin

  val GenerationFeatureFile = StringFileArtifact(
    ".atomist/tests/project/Generation.feature",
    GenerationFeature)

  /**
    * @param params map to string representation of param, e.g. including "
    */
  def generationTest(gen: String, params: Map[String,String]): String =
    s"""
       |import {Project} from "@atomist/rug/model/Core"
       |import {ProjectGenerator} from "@atomist/rug/operations/ProjectGenerator"
       |import {Given,When,Then,ProjectScenarioWorld} from "@atomist/rug/test/project/Core"
       |
      |When("run simple generator", (p, w) => {
       |  let world = w as ProjectScenarioWorld
       |  let g = world.generator("$gen")
       |  world.generateWith(g, {${params.map(p => s"${p._1}: ${p._2}").mkString(", ")}})
       |})
       |Then("parameters were valid", (p, world) => !world.invalidParameters())
       |Then("we have Anders", p => {
       |   let f = p.findFile("src/from/typescript")
       |   return f != null && f.content().indexOf("Anders") > -1
       |})
       |Then("we have file from start project", p => {
       |   return p.findFile("pom.xml") != null
       |})
    """.stripMargin

  def generateWithInvalidParameters(gen: String, params: Map[String,String]): String =
    s"""
       |import {Project} from "@atomist/rug/model/Core"
       |import {ProjectGenerator} from "@atomist/rug/operations/ProjectGenerator"
       |import {Given,When,Then,ProjectScenarioWorld} from "@atomist/rug/test/project/Core"
       |
       |When("run simple generator", (p, w) => {
       |  let world = w as ProjectScenarioWorld
       |  let g = world.generator("$gen")
       |  world.generateWith(g, {${params.map(p => s"${p._1}: ${p._2}").mkString(", ")}})
       |})
    """.stripMargin


}
