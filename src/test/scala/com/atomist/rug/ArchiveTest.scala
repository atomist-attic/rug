package com.atomist.rug

import com.atomist.param.SimpleParameterValues
import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.project.edit._
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source._
import org.scalatest.{FlatSpec, Ignore, Matchers}

/**
  * Test for rug archive format
  */
// TODO convert to TypeScript
@Ignore
class ArchiveTest extends FlatSpec with Matchers {

  val atomistConfig = DefaultAtomistConfig

  private def toArchive(files: Seq[FileArtifact]): ArtifactSource = {
    new SimpleFileBasedArtifactSource("rugs", files)
  }

  it should "ignore editors in root directory" in {
    val tss = TestUtils.resourcesInPackage(this).filter(
      _.path == "",
      _.name == "K8Redeploy.ts")
    assert(tss.totalFileCount === 1)
    val as = TypeScriptBuilder.compileWithModel(tss)
    val rugs = RugArchiveReader(as)
    rugs.allRugs shouldBe empty
  }

  val prog1 = StringFileArtifact(atomistConfig.editorsRoot + "/Dude.rug",
    s"""
       |editor Dude
       |
       |with Project p
       |do
       |  merge "template.mustache" "dude.txt"
      """.stripMargin)

  val prog2 = StringFileArtifact(atomistConfig.editorsRoot + "/Donny.rug",
    """
      |editor Donny
      |
      |with Project p
      |do
      |  merge "template.mustache" "dude.txt";
    """.stripMargin
  )

  it should "find editor and access template under .atomist" in {
    val canAccess = true
    verifyFileWithArchiveAccess(Seq(prog1, prog2), canAccess)
  }

  it should "find editor and access template under root" in {
    val canAccess = false
    verifyFileWithArchiveAccess(Seq(prog1, prog2), canAccess)
  }

  private def verifyFileWithArchiveAccess(files: Seq[FileArtifact], useDotAtomist: Boolean) {
    val rootToUse = if (useDotAtomist) atomistConfig.templatesRoot else atomistConfig.templatesDirectory
    val as = TestUtils.rugsInSideFileAsArtifactSource(this, "Merge.ts") +
      StringFileArtifact(s"$rootToUse/template.mustache", "content")
    tryMod(as, "Merge", as) match {
      case sm: SuccessfulModification =>
        sm.result.findFile("dude.txt").isDefined
      case _ => ???
    }
  }

  private def tryMod(rugAs: ArtifactSource, editorName: String, project: ArtifactSource): ModificationAttempt = {
    val rugs = RugArchiveReader(rugAs)
    val eds = rugs.editors
    val peO = eds.find(_.name equals editorName)
    if (peO.isEmpty)
      fail(s"Did not find editor with name '$editorName': Have [${eds.map(_.name)}] " +
        s"\n${ArtifactSourceUtils.prettyListFiles(rugAs)}")
    peO.get.modify(project, SimpleParameterValues(Map[String, String](
    )))
  }
}
