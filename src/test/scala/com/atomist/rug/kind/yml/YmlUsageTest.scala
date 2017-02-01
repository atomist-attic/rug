package com.atomist.rug.kind.yml

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit.{NoModificationNeeded, SuccessfulModification}
import com.atomist.rug.DefaultRugPipeline
import com.atomist.rug.InterpreterRugPipeline.DefaultRugArchive
import com.atomist.source.{ArtifactSource, EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class YmlUsageTest extends FlatSpec with Matchers {
  import com.atomist.rug.TestUtils._

  val xYml =
    """
      |group: queen
      |artifact: "A Night at the Opera"
      |dependencies:
      |- "Death on Two Legs (Dedicated to....)"
      |- "Lazing on a Sunday Afternoon"
      |- "I'm in Love with My Car"
      |- "You're My Best Friend"
      |- "'39"
      |- "Sweet Lady"
      |- "Seaside Rendezvous"
      |- "The Prophet's Song"
      |- "Love of My Life"
      |- "Good Company"
      |- "Bohemian Rhapsody"
      |- "God Save the Queen"
      |common: everywhere
    """.stripMargin

  val yYml =
    """
      |artist: Paul Simon
      |album: Graceland
      |songs:
      |- The Boy in the Bubble
      |- Graceland
      |- I Know What I Know
      |- Gumboots
      |- Diamonds on the Soles of Her Shoes
      |- You Can Call Me Al
      |- Under African Skies
      |- Homeless
      |- Crazy Love, Vol. II
      |- That Was Your Mother
      |- All Around the World or the Myth of Fingerprints
      |common: everywhere
    """.stripMargin

  val zYml =
    """
      |something: completely different
      |this:
      |  structure:
      |    is:
      |    - more
      |    - nested
      |that:
      |- is
      |- not
      |some:
      |  nesting: one level
      |common: everywhere
    """.stripMargin

  val singleAS = new SimpleFileBasedArtifactSource("single",
    Seq(StringFileArtifact("x.yml", xYml))
  )

  val yamlAS = new SimpleFileBasedArtifactSource("triple",
    Seq(
      StringFileArtifact("x.yml", xYml),
      StringFileArtifact("src/main/y.yml", yYml),
      StringFileArtifact("target/build/z.yml", zYml)
    )
  )

  val fullAS = new SimpleFileBasedArtifactSource("full",
    Seq(
      StringFileArtifact("README.md", "This is a README\n"),
      StringFileArtifact("x.yml", xYml),
      StringFileArtifact("src/main/y.yml", yYml),
      StringFileArtifact("src/main/not-yml.txt", "Not YAML file.\n\nWe should not find this.\n"),
      StringFileArtifact("target/build/z.yml", zYml)
    )
  )

  val allAS = Seq((singleAS, 1), (yamlAS, 3), (fullAS, 3))

  private def runProgAndCheck(prog: String, as: ArtifactSource, mods: Int): ArtifactSource = {
    val progArtifact: ArtifactSource = new SimpleFileBasedArtifactSource(DefaultRugArchive,
      StringFileArtifact(new DefaultRugPipeline().defaultFilenameFor(prog), prog)
    )

    val modAttempt = attemptModification(progArtifact, as, EmptyArtifactSource(""),
      SimpleProjectOperationArguments("", Map.empty[String,Object]))

    modAttempt match {
      case sm: SuccessfulModification if sm.result.cachedDeltas.size == mods =>
        sm.result
      case _: NoModificationNeeded  if mods == 0 =>
        as
      case ma =>
        fail(s"incorrect number of changes: $mods; $prog; $as; $ma")
        as
    }
  }

  it should "get group value with no change with native Rug function" in {
    val prog =
      """
        |editor YmlEdit
        |
        |with Yml x when path = "x.yml"
        |  do valueOf "group"
      """.stripMargin
    allAS.foreach(asChanges => runProgAndCheck(prog, asChanges._1, 0))
  }

  it should "change group value with native Rug function" in {
    val prog =
      """
        |editor YmlEdit
        |
        |with Yml x when path = "x.yml"
        |  do updateKey "group" "Marx Brothers"
      """.stripMargin
    allAS.foreach(asChanges => runProgAndCheck(prog, asChanges._1, 1))
  }

  it should "change group value only if exists with native Rug function" in {
    val prog =
      """
        |editor YmlEdit
        |
        |with Yml x
        |  do updateKey "group" "Marx Brothers"
      """.stripMargin
    allAS.foreach(asChanges => runProgAndCheck(prog, asChanges._1, 1))
  }

  it should "change group value in all files with native Rug function" in {
    val prog =
      """
        |editor YmlEdit
        |
        |with Yml x
        |  do updateKey "common" "Be"
      """.stripMargin
    allAS.foreach(asChanges => runProgAndCheck(prog, asChanges._1, asChanges._2))
  }

  it should "get group value via tree expression with no change with native Rug function" in {
    val prog =
      """
        |editor YmlEdit
        |
        |let pe = $(/*[@name='x.yml']/Yml())
        |
        |with pe
        |  do valueOf "group"
      """.stripMargin
    allAS.foreach(asChanges => runProgAndCheck(prog, asChanges._1, 0))
  }

  it should "change group value via tree expression with native Rug function" in {
    val prog =
      """
        |editor YmlEdit
        |
        |let pe = $(/*[@name='x.yml']/Yml())
        |
        |with pe
        |  do updateKey "group" "Marx Brothers"
      """.stripMargin
    allAS.foreach(asChanges => runProgAndCheck(prog, asChanges._1, 1))
  }

  it should "change group value via tree expression only if exists with native Rug function" in {
    val prog =
      """
        |editor YmlEdit
        |
        |let pe = $(/Yml())
        |
        |with pe
        |  do updateKey "group" "Marx Brothers"
      """.stripMargin
    allAS.foreach(asChanges => runProgAndCheck(prog, asChanges._1, 1))
  }

  it should "change group value via tree expression in all files with native Rug function" in {
    val prog =
      """
        |editor YmlEdit
        |
        |let pe = $(/Yml())
        |
        |with pe
        |  do updateKey "common" "Be"
      """.stripMargin
    allAS.foreach(asChanges => runProgAndCheck(prog, asChanges._1, asChanges._2))
  }

  it should "make no changes when tree expression does not match any files" in {
    val prog =
      """
        |editor YmlEdit
        |
        |let pe = $(/src/*[@name='x.yml']/Yml())
        |
        |with pe
        |  do updateKey "common" "Be"
      """.stripMargin
    allAS.foreach(asChanges => runProgAndCheck(prog, asChanges._1, 0))
  }
}
