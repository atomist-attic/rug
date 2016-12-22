package com.atomist.rug.kind.rug

import com.atomist.source.{ArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class RugMutableViewTest extends FlatSpec
  with Matchers
  with RugEditorTestHelper
  with TypeScriptEditorTestHelper
{

  // the editor I want to work.
  val ConvertRugToTsEditor =
    """editor ConvertRugToTypescript
      |
      |# TODO: what is the format ... and can I add to these in a kind like RugType
      |param rug_name: ^.*$
      |
      |with RugArchiveProject p begin
      |  do eval { print("The rug name is " + rug_name) }
      |  with Rug r when r.name = rug_name begin
      |    do eval { print("Changing rug " + r.name() ) }
      |    do eval { p.convertToTypeScript(r) }
      |  end
      |end
    """.stripMargin

  val StartingRug =
    """editor BananaToCarrot
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
    tsEditorFile.isDefined should be(true)
    val tsEditor = tsEditorFile.get.content
    println("it turned into: " + tsEditor)

    val resultOfTsEditor = executeTypescript("BananaToCarrot.ts", tsEditor, InputProject)

    resultOfTsEditor should be(resultOfRugEditor)

  }

}
