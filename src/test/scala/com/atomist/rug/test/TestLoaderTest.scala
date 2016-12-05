package com.atomist.rug.test

import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.source.{EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class TestLoaderTest extends FlatSpec with Matchers {

  val ac = DefaultAtomistConfig
  val testLoader = new TestLoader(ac)

  it should "not find scenarios in empty ArtifactSource" in {
    testLoader.loadTestScenarios(EmptyArtifactSource("")).size should be (0)
  }

  val foobarScenario =
    """
      |scenario Foobar
      |
      |given
      |   test = "content"
      |
      | DoSomething
      |
      |then
      |  contentEquals test "class Cat {}"
    """.stripMargin

  val bazScenario =
    """
      |scenario Baz
      |
      |given
      |   test = "content"
      |
      | DoSomething
      |
      |then
      |  NoChange
    """.stripMargin

  it should "ignore test scenarios in root" in {
    testLoader.loadTestScenarios(new SimpleFileBasedArtifactSource("",
      Seq(
        StringFileArtifact("foo.rt", foobarScenario),
        StringFileArtifact("baz.rt", bazScenario)))
    ).size should be (0)
  }

  it should s"find test scenarios under ${ac.testsRoot}" in {
    val scenarios = testLoader.loadTestScenarios(new SimpleFileBasedArtifactSource("",
      Seq(
        StringFileArtifact(s"${ac.testsRoot}/foo.rt", foobarScenario),
        StringFileArtifact(s"${ac.testsRoot}/deeper/baz.rt", bazScenario)))
    )
    scenarios.size should be (2)
    scenarios.map(sc => sc.name).toSet should equal (Set("Foobar", "Baz"))
  }

  it should s"find test scenarios under ${DefaultAtomistConfig.testsDirectory}" in {
    val scenarios = testLoader.loadTestScenarios(new SimpleFileBasedArtifactSource("",
      Seq(
        StringFileArtifact(s"${ac.testsDirectory}/foo.rt", foobarScenario),
        StringFileArtifact(s"${ac.testsDirectory}/deeper/baz.rt", bazScenario)))
    )
    scenarios.size should be (2)
    scenarios.map(sc => sc.name).toSet should equal (Set("Foobar", "Baz"))
  }
}
