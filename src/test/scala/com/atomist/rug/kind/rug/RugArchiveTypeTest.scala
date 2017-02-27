package com.atomist.rug.kind.rug

import java.nio.file.{Files, Paths}

import com.atomist.source.{ArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}
import scala.collection.JavaConverters._

class RugArchiveTypeTest extends FlatSpec
  with Matchers
  with RugEditorTestHelper
  with TypeScriptEditorTestHelper {

  // the editor I want to work.
  val ConvertRugToTsEditor =
    """editor ConvertRugToTypescript
      |
      |# TODO: what is the format ... and can I add to these in a kind like RugType
      |param rug_name: ^.*$
      |
      |with RugArchiveProject p begin
      |  #do eval { print("The rug name is " + rug_name) }
      |  with Editor r when r.name = rug_name begin
      |    #do eval { print("Changing rug " + r.name() ) }
      |    do r.convertToTypeScript
      |  end
      |end
    """.stripMargin

  val StartingRug =
    """@description "Paint it orange and make it crunchy"
      |editor BananaToCarrot
      |
      |with Project p
      |  with File f
      |     do replace "banana" "carrots"
    """.stripMargin

  val StartingProject =
    new SimpleFileBasedArtifactSource("my-rug-archive",
      Seq(StringFileArtifact(".atomist/editors/BananaToCarrot.rug", StartingRug
    )))

  val InputProject =
    new SimpleFileBasedArtifactSource("my-rug-archive",
      Seq(StringFileArtifact("whatever.txt", "armadillo banana carrots"
      )))

  it should "convert a rug to TS" in {
    val resultOfRugEditor = executeRug(StartingRug, InputProject)

    val result: ArtifactSource = executeRug(ConvertRugToTsEditor,
      StartingProject, Map("rug_name" -> "BananaToCarrot"))

    val tsEditorFile = result.findFile(".atomist/editors/BananaToCarrot.ts")
    assert(tsEditorFile.isDefined === true)
    val tsEditor = tsEditorFile.get.content
    println("it turned into: " + tsEditor)
    /* writing it out makes it easier for debugging; can open it in VSCode */
    Files.write(Paths.get("src/test/resources/com/atomist/rug/kind/rug/actual.ts"), tsEditor.getBytes())

    val desired = Files.readAllLines(Paths.get("src/test/resources/com/atomist/rug/kind/rug/BananaToCarrot.ts")).asScala.mkString("\n")

    tsEditor should be(desired)

    val resultOfTsEditor = executeTypescript("BananaToCarrot", tsEditor, InputProject)

    singleFileArtifactSourcesAreEquivalent(resultOfTsEditor, resultOfRugEditor) should be(true)

  }

  def singleFileArtifactSourcesAreEquivalent(as1: ArtifactSource, as2: ArtifactSource): Boolean = {
    assert(as1.allFiles.size === 1)
    assert(as2.allFiles.size === 1)

    val as1Name = as1.allFiles.head.path
    val as2Name = as2.allFiles.head.path
    as1Name should be(as2Name)

    val as1Contents = as1.allFiles.head.content
    val as2Contents = as2.allFiles.head.content
    as1Contents should be(as2Contents)

    true
  }

}
