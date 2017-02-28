package com.atomist.rug.kind.rug

import java.nio.file.{Files, Paths}

import com.atomist.param.SimpleParameterValues
import com.atomist.project.edit.{ProjectEditor, SuccessfulModification}
import com.atomist.rug.DefaultRugPipeline
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.parser.ParserCombinatorRugParser
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source.{ArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConverters._

class RugArchiveTypeTest extends FlatSpec
  with Matchers
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
      |@tag "vegetable"
      |@tag "fruit"
      |editor BananaToCarrot
      |
      |@displayName "Banana Peel"
      |@description "peel of the banana"
      |@validInput "slippery but protective"
      |@minLength 1
      |@maxLength 100
      |param peel: @any
      |
      |@optional
      |@default "golden"
      |param hue: "^.*$"
      |
      |with Project p begin
      |  with File
      |     do replace "banana" "carrots"
      |  do copyEditorBackingFileOrFail "source_file" "to/path"
      |end
    """.stripMargin

  val RugArchiveUnderTest =
    new SimpleFileBasedArtifactSource("my-rug-archive",
      Seq(StringFileArtifact(".atomist/editors/ConvertRugToTypescript.rug", ConvertRugToTsEditor)))

  val StartingProject =
    new SimpleFileBasedArtifactSource("my-rug-archive",
      Seq(StringFileArtifact(".atomist/editors/BananaToCarrot.rug", StartingRug),
        StringFileArtifact("source_file", "some stuff is in here")))

  val InputProject =
    new SimpleFileBasedArtifactSource("my-rug-archive",
      Seq(StringFileArtifact("whatever.txt", "armadillo banana carrots"
      )))

  it should "convert a rug to TS" in {
    val resultOfRugEditor = executeRug(StartingProject, InputProject, Map("peel" -> "flecked with brown"))

    val result: ArtifactSource = executeRug(RugArchiveUnderTest,
      StartingProject, Map("rug_name" -> "BananaToCarrot"))

    val tsEditorFile = result.findFile(".atomist/editors/BananaToCarrot.ts")
    assert(tsEditorFile.isDefined === true)
    val tsEditor = tsEditorFile.get.content
    println("it turned into: " + tsEditor)
    /* writing it out makes it easier for debugging; can open it in VSCode */
    Files.write(Paths.get("src/test/resources/com/atomist/rug/kind/rug/actual.ts"), tsEditor.getBytes())

    val desired = Files.readAllLines(Paths.get("src/test/resources/com/atomist/rug/kind/rug/BananaToCarrot.ts")).asScala.mkString("\n")

    tsEditor should be(desired)

    val compiledTsArchive = TypeScriptBuilder.compileWithModel(result)
    val resultOfTsEditor = executeTypescript(compiledTsArchive, InputProject, Map("peel" -> "flecked with brown"))

    artifactSourcesAreEquivalent(resultOfTsEditor, resultOfRugEditor) should be(true)

  }

  def artifactSourcesAreEquivalent(as1: ArtifactSource, as2: ArtifactSource): Boolean = {
    assert(as1.allFiles.size === as2.allFiles.size)

    val files1 = as1.allFiles.sortBy(_.path)
    val files2 = as2.allFiles.sortBy(_.path)

    files1.zip(files2).foreach {
      case (file1, file2) =>

        val as1Name = file1.path
        val as2Name = file2.path
        as1Name should be(as2Name)

        val as1Contents = file1.content
        val as2Contents = file2.content
        as1Contents should be(as2Contents)
    }

    true
  }


  def executeRug(rugArchive: ArtifactSource, startingProject: ArtifactSource,
                 params: Map[String, String] = Map()): ArtifactSource = {
    val runtime = new DefaultRugPipeline(DefaultTypeRegistry)

    val eds = runtime.create(rugArchive, None)
    assert(eds.size === 1)
    val pe = eds.head.asInstanceOf[ProjectEditor]

    val r = pe.modify(startingProject, SimpleParameterValues(params))
    r match {
      case sm: SuccessfulModification =>
        sm.result
      case um =>
        fail("This modification was not successful: " + um)
    }
  }

  private def parseRugForEditorName(program: String) =
    new ParserCombinatorRugParser().parse(program).head.name


}
