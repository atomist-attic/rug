package com.atomist.rug

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.project.edit._
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.source._
import org.scalatest.{FlatSpec, Matchers}

/**
  * Test for rug archive format
  */
class ArchiveTest extends FlatSpec with Matchers {

  val atomistConfig = DefaultAtomistConfig

  private def toArchive(files: Seq[FileArtifact]): ArtifactSource = {
    new SimpleFileBasedArtifactSource("rugs", files)
  }

  it should "ignore editors in root directory" in {
    val prog =
      """
        |@description "Update Kube spec to redeploy a service"
        |editor Redeploy
        |
        |with file f
        | when { f.name().contains("80-deployment") }
        |do
        |  replace ".*" "foo"
      """.stripMargin
    val as = new SimpleFileBasedArtifactSource("",
      StringFileArtifact("Redeploy.rug", prog))
    val runtime = new DefaultRugPipeline(DefaultTypeRegistry)
    val eds = runtime.create(as, None, Nil)
    eds shouldBe (empty)
  }

  it should s"find single file under ${atomistConfig.editorsRoot}" in {
    val prog =
      """
        |@description "Update Kube spec to redeploy a service"
        |editor Redeploy
        |
        |with file f
        | when { f.name().contains("80-deployment") }
        |do
        |  replace ".*" "foo"
      """.stripMargin
    val f = StringFileArtifact(atomistConfig.editorsRoot + "/Redeploy.rug", prog)
    val as = toArchive(Seq(f))
    tryMod(as, "Redeploy", as) match {
      case n: NoModificationNeeded =>
    }
  }

  it should "accept executor passed in as string" in pendingUntilFixed {
    val ex =
      s"""
         |executor AddSomeCaspar
         |with services s
         | editWith Caspar
         |
         |editor Caspar
         |with project
         | do addFile "Caspar" 'What is this, the high hat?'
      """.stripMargin
    val f = StringFileArtifact(atomistConfig.editorsRoot + "/AddSomeCaspar.rug", ex)
    val as = toArchive(Seq(f))
    tryMod(as, "AddSomeCaspar", as) match {
      case sm: SuccessfulModification =>
    }
  }

  it should s"find single file under ${atomistConfig.editorsDirectory}" in {
    val prog =
      """
        |@description "Update Kube spec to redeploy a service"
        |editor Redeploy
        |
        |with file f
        | when { f.name().contains("80-deployment") }
        |do
        |  replace ".*" "foo"
      """.stripMargin
    val f = StringFileArtifact(atomistConfig.editorsDirectory + "/Redeploy.rug", prog)
    val as = toArchive(Seq(f))
    tryMod(as, "Redeploy", as) match {
      case n: NoModificationNeeded =>
    }
  }

  val prog1 = StringFileArtifact(atomistConfig.editorsRoot + "/Dude.rug",
    s"""
       |editor Dude
       |
       |with project p
       |do
       |  merge "template.vm" "dude.txt"
      """.stripMargin)

  val prog2 = StringFileArtifact(atomistConfig.editorsRoot + "/Donny.rug",
    """
      |editor Donny
      |
      |with project p
      |do
      |  merge "template.vm" "dude.txt";
    """.stripMargin
  )

  it should "find 2 editors in separate files and access template under .atomist" in
    verify2FilesWithArchiveAccess(Seq(prog1, prog2), true)

  it should "find 2 editors in separate files and access template under root" in
    verify2FilesWithArchiveAccess(Seq(prog1, prog2), false)

  private def verify2FilesWithArchiveAccess(files: Seq[FileArtifact], useDotAtomist: Boolean) {
    val rootToUse = if (useDotAtomist) atomistConfig.templatesRoot else atomistConfig.templatesDirectory
    val as = toArchive(files) + StringFileArtifact(s"$rootToUse/template.vm", "content")
    tryMod(as, "Dude", as) match {
      case sm: SuccessfulModification =>
        sm.result.findFile("dude.txt").isDefined
    }
    tryMod(as, "Donny", as) match {
      case sm: SuccessfulModification =>
        sm.result.findFile("donny.txt").isDefined
    }
  }

  private def tryMod(rugAs: ArtifactSource, editorName: String, project: ArtifactSource): ModificationAttempt = {
    val runtime = new DefaultRugPipeline(DefaultTypeRegistry)
    val eds = runtime.create(rugAs, None)
    val peO = eds.find(_.name equals editorName)
    if (peO.isEmpty)
      fail(s"Did not find editor with name '$editorName': Have [${eds.map(_.name)}] " +
        s"\n${ArtifactSourceUtils.prettyListFiles(rugAs)}")

    peO.get.asInstanceOf[ProjectEditor].modify(project, SimpleProjectOperationArguments("", Map[String, String](
    )))
  }
}
