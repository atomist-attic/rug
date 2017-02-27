package com.atomist.rug.test.gherkin

import com.atomist.source.{EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class GherkinReaderTest extends FlatSpec with Matchers {

  import GherkinReaderTest._

  it should "handle empty ArtifactSource" in {
    GherkinReader.findFeatures(EmptyArtifactSource("")).isEmpty should be (true)
  }

  it should "parse a simple file" in {
    val as = SimpleFileBasedArtifactSource(SimpleFeatureFile)
    GherkinReader.findFeatures(as).toList match {
      case feature :: Nil =>
        assert(feature.feature.getChildren.size === 1)
        val scenario = feature.feature.getChildren.get(0)
        assert(scenario.getSteps.size() === 4)
      case wtf => fail(s"Unexpected: $wtf")
    }
  }

  it should "parse file with 2 scenarios" in {
    val as = SimpleFileBasedArtifactSource(TwoScenarioFeatureFile)
    GherkinReader.findFeatures(as).toList match {
      case feature :: Nil =>
        assert(feature.feature.getChildren.size === 2)
      case wtf => fail(s"Unexpected: $wtf")
    }
  }

}


object GherkinReaderTest {

  val Simple =
    """
      |Feature: Do anything at all
      | This is a test
      | to see if
      | Gherkin is a good option
      |
      |Scenario: I want to parse a file
      | Given an empty project
      | Given a file
      | When it is edited
      | Then happiness ever after
      |
    """.stripMargin

  val FailingSimpleTs =
    """
      |import {Project} from "@atomist/rug/model/Core"
      |import {ProjectEditor} from "@atomist/rug/operations/ProjectEditor"
      |import {Given,When,Then,Result} from "@atomist/rug/test/Core"
      |
      |Given("a file", p => {
      | p.addFile("Gough", "Maintain the rage")
      |})
      |When("it is edited", p => {
      | p.addFile("Malcolm", "Life wasn't meant to be easy")
      | p.deleteFile("Gough")
      |})
      |Then("happiness ever after", p => p.fileExists("Gough"))
    """.stripMargin

  val PassingSimpleTs =
    """
      |import {Project} from "@atomist/rug/model/Core"
      |import {ProjectEditor} from "@atomist/rug/operations/ProjectEditor"
      |import {Given,When,Then,Result} from "@atomist/rug/test/Core"
      |
      |Given("a file", p => {
      | p.addFile("Gough", "Maintain the rage")
      |})
      |When("it is edited", p => {})
      |Then("happiness ever after", p => p.fileExists("Gough"))
    """.stripMargin

  val EditorWithoutParametersTs =
    """
      |import {Project} from "@atomist/rug/model/Core"
      |import {ProjectEditor} from "@atomist/rug/operations/ProjectEditor"
      |import {Given,When,Then,Result} from "@atomist/rug/test/Core"
      |
      |import {AlpEditor} from "../editors/AlpEditor"
      |
      |Given("a file", p => {
      | p.addFile("Gough", "Maintain the rage")
      |})
      |When("it is edited", p => {
      |  let e = new AlpEditor()
      |  e.edit(p)
      |})
      |Then("happiness ever after", p => {
      |   return p.fileExists("Paul")
      |})
    """.stripMargin

  val EditorWithParametersTs =
    """
      |import {Project} from "@atomist/rug/model/Core"
      |import {ProjectEditor} from "@atomist/rug/operations/ProjectEditor"
      |import {Given,When,Then,Result} from "@atomist/rug/test/Core"
      |
      |import {AlpEditor} from "../editors/AlpEditor"
      |
      |Given("a file", p => {
      | p.addFile("Gough", "Maintain the rage")
      |})
      |When("it is edited", p => {
      |  let e = new AlpEditor()
      |  // Simply inject property
      |  e.heir = "Paul"
      |  e.edit(p)
      |})
      |Then("happiness ever after", p => {
      |   return p.fileExists("Paul")
      |})
    """.stripMargin


  val PassingSimpleTsFile = StringFileArtifact(".atomist/test/Simple_definitions.ts", PassingSimpleTs)

  val FailingSimpleTsFile = StringFileArtifact(".atomist/test/Simple_definitions.ts", FailingSimpleTs)

  val EditorWithoutParametersTsFile = StringFileArtifact(".atomist/test/Simple_definitions.ts", EditorWithoutParametersTs)

  val EditorWithParametersTsFile = StringFileArtifact(".atomist/test/Simple_definitions.ts", EditorWithParametersTs)

  val TwoScenarios =
    """
      |Feature: Do anything at all
      | This is a test
      | to see if
      | Gherkin is a good option
      |
      |Scenario: I want to parse a file
      | Given a file
      | When it is edited
      | Then happiness ever after
      |
      |Scenario: I want to go home early
      | Given a file
      | When it is edited
      | Then everything's done
    """.stripMargin

  val SimpleFeatureFile = StringFileArtifact(".atomist/test/Simple.feature", Simple)

  val TwoScenarioFeatureFile = StringFileArtifact(".atomist/test/Two.feature", TwoScenarios)

}