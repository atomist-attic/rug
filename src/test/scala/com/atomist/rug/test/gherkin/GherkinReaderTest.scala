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

  it should "not load features and scenarios from the node_modules directory" in {
    val as = SimpleFileBasedArtifactSource(SimpleFeatureFile).withPathAbove(".atomist/node_modules/blah")
    assert(GherkinReader.findFeatures(as).size == 0)
  }
}

object GherkinReaderTest {

  val Simple =
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
      |    Then parameters were valid
      |    Then changes were made
      |    Then one edit was made
      |    Then the rage is maintained
      |    Then the rage has a name
    """.stripMargin

  val TwoScenarios =
    """
      |Feature: Do anything at all
      |  This is a test
      |  to see if
      |  Gherkin is a good option
      |
      |  Scenario: I want to parse a file
      |    Given a file
      |    When politics takes its course
      |    Then the rage is maintained
      |    Then the rage has a name
      |
      |  Scenario: I want to go home early
      |    Given a file
      |    When politics takes its course
      |    Then everything's done
    """.stripMargin

  val NotImplementedGivenFeature =
    """
      |Feature: Australian political history
      |  This is a test
      |  to demonstrate that the Gherkin DSL
      |  is a good fit for Rug BDD testing
      |
      |  Scenario: Australian politics, 1972-1991
      |    Given an empty project
      |    Given a visionary leader
      |    Given rising inequality
      |    When politics takes its course
      |    Then parameters were valid
      |    Then changes were made
      |    Then one edit was made
      |    Then the rage is maintained
      |    Then the rage has a name
    """.stripMargin

  val NotImplementedWhenFeature =
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
      |    When income disparity widens
      |    Then parameters were valid
      |    Then changes were made
      |    Then one edit was made
      |    Then the rage is maintained
      |    Then the rage has a name
    """.stripMargin

  val SimpleFeatureFile = StringFileArtifact(".atomist/tests/project/Simple.feature", Simple)

  val TwoScenarioFeatureFile = StringFileArtifact(".atomist/tests/project/Two.feature", TwoScenarios)

  val NotImplementedGivenFeatureFile = StringFileArtifact(".atomist/tests/project/NotImplemented.feature",
    NotImplementedGivenFeature)

  val NotImplementedWhenFeatureFile = StringFileArtifact(".atomist/tests/project/NotImplemented.feature",
    NotImplementedWhenFeature)

}
