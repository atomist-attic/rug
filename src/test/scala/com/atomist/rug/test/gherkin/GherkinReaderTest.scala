package com.atomist.rug.test.gherkin

import com.atomist.source.{EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class GherkinReaderTest extends FlatSpec with Matchers {

  import GherkinReaderTest._

  "Gherkin reader" should "handle empty ArtifactSource" in {
    GherkinReader.findFeatures(EmptyArtifactSource("")).isEmpty should be (true)
  }

  it should "parse a simple file" in {
    val as = SimpleFileBasedArtifactSource(SimpleFeatureFile)
    GherkinReader.findFeatures(as).toList match {
      case feature :: Nil =>
        assert(feature.feature.getChildren.size === 1)
        val scenario = feature.feature.getChildren.get(0)
        assert(scenario.getSteps.size() >= 4)
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
      |Feature: Australian political history
      | This is a test
      | to demonstrate that the Gherkin DSL
      | is a good fit for Rug BDD testing
      |
      |Scenario: Australian politics, 1972-1991
      | Given an empty project
      | Given a visionary leader
      | When politics takes its course
      | Then changes were made
      | Then one edit was made
      | Then the rage is maintained
    """.stripMargin

  val TwoScenarios =
    """
      |Feature: Do anything at all
      | This is a test
      | to see if
      | Gherkin is a good option
      |
      |Scenario: I want to parse a file
      | Given a file
      | When politics takes its course
      | Then the rage is maintained
      |
      |Scenario: I want to go home early
      | Given a file
      | When politics takes its course
      | Then everything's done
    """.stripMargin

  val SimpleFeatureFile = StringFileArtifact(".atomist/test/project/Simple.feature", Simple)

  val TwoScenarioFeatureFile = StringFileArtifact(".atomist/test/project/Two.feature", TwoScenarios)

}