package com.atomist.rug.kind.elm

import com.atomist.rug.DefaultRugPipeline
import com.atomist.rug.kind.elm.ElmTypeUsageTest.TestDidNotModifyException
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.OptionValues._

class PositivePredicateTest extends FlatSpec with Matchers {

  import ElmTypeUsageTest.elmExecute

  val editor =
    """editor Baby
      |
      |precondition Yes
      |
      |with ElmModule e
      |  do updateImport from "Carrot" to "Kiwifruit"
      |
      |predicate Yes
      |
      |with Project p
      |  when fileExists "Banana.elm"
    """.stripMargin

  val original =
    """module Banana exposing (..)
      |
      |import Carrot
      | """.stripMargin

  val expected =
    """module Banana exposing (..)
      |
      |import Kiwifruit
      |""".stripMargin

  it should "run the editor when the predicate passes" in {
    val source = StringFileArtifact("Banana.elm", original)

    val elmProject = new SimpleFileBasedArtifactSource("", Seq(source))

    val result = elmExecute(elmProject, editor, Map[String, String](),
      runtime = new DefaultRugPipeline())

    val carrotContent = result.findFile(s"Banana.elm").value.content
    // TODO: remove the trims! this finds the newline problem
    carrotContent.trim should equal(expected.trim)
  }

  it should "not delete the trailing newline" in pendingUntilFixed {
    val source = StringFileArtifact("Banana.elm", original)

    val elmProject = new SimpleFileBasedArtifactSource("", Seq(source))

    val result = elmExecute(elmProject, editor, Map[String, String](),
      runtime = new DefaultRugPipeline())

    val carrotContent = result.findFile(s"Banana.elm").value.content
    // TODO: remove the trims! this finds the newline problem
    carrotContent should equal(expected)
  }
}

class NegativePredicateTest extends FlatSpec with Matchers {

  import ElmTypeUsageTest.elmExecute

  val editor =
    """editor Baby
      |
      |precondition No
      |
      |with ElmModule e
      |  do updateImport from "Carrot" to "Kiwifruit"
      |
      |predicate No
      |
      |with Project p
      |  when not fileExists "Banana.elm"
    """.stripMargin

  val original =
    """module Banana exposing (..)
      |
      |import Carrot
      | """.stripMargin

  val expected =
    """module Banana exposing (..)
      |
      |import Kiwifruit
      |""".stripMargin

  it should "does not modify when the predicate fails" in {
    val source = StringFileArtifact("Banana.elm", original)

    val elmProject = new SimpleFileBasedArtifactSource("", Seq(source))

    an [TestDidNotModifyException] should be thrownBy {
      elmExecute(elmProject, editor, Map[String, String](),
        runtime = new DefaultRugPipeline())
    }
  }
}
