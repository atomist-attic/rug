package com.atomist.rug.kind.core

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.rug.kind.java.JavaClassTypeUsageTest
import com.atomist.rug.runtime.SimpleFunctionInvocationContext
import com.atomist.rug.spi.InstantEditorFailureException
import com.atomist.source.{EmptyArtifactSource, FileArtifact, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConversions._

class ProjectMutableViewTest extends FlatSpec with Matchers {

  val atomistConfig: AtomistConfig = DefaultAtomistConfig

  val First =
    """Hello {{name}}
      |You have just won {{value}} dollars!
      |{{#in_ca}}
      |Well, {{taxed_value}} dollars, after taxes.{{/in_ca}}""".stripMargin

  val FirstPoa = SimpleProjectOperationArguments("", Map[String, String](
    "name" -> "Chris",
    "value" -> "10000",
    "taxed_value" -> "6,000",
    "in_ca" -> "true"
  ))

  // TODO should be 6000.0 in samples
  val FirstExpected =
    """Hello Chris
      |You have just won 10000 dollars!
      |Well, 6,000 dollars, after taxes.""".stripMargin

  val templateName = "first.mustache"
  val static1 = StringFileArtifact("static1", "test")
  val doubleDynamic = StringFileArtifact("location_was_{{in_ca}}.txt_.mustache", First)
  val straightTemplate = StringFileArtifact(templateName, First)

  val backingTemplates = new SimpleFileBasedArtifactSource("",
    Seq(
      straightTemplate,
      static1,
      doubleDynamic
    )).withPathAbove(atomistConfig.templatesRoot)

  it should "handle a simple regexpReplace of contents in a README file" in {
    val project = JavaClassTypeUsageTest.NewSpringBootProject
    val pmv = new ProjectMutableView(backingTemplates, project)
    val regexToReplace = "To run locally"
    val replacementText = "To run somewhere else..."
    pmv.regexpReplace(regexToReplace, replacementText)
    pmv.dirty should be(true)
    val r = pmv.currentBackingObject
    val readmeFile = r.findFile("README.md").get
    readmeFile.content.contains(replacementText) should be(true)
  }

  it should "correctly handle a regexReplace that has been accidentally specified with an @regex lookup" in {
    val project = JavaClassTypeUsageTest.NewSpringBootProject
    val pmv = new ProjectMutableView(backingTemplates, project)
    val regexToReplace = "@project_name"
    val replacementText = "To run somewhere else on @ symbol..."
    pmv.regexpReplace(regexToReplace, replacementText)
    pmv.dirty should be(true)
    val r = pmv.currentBackingObject
    val readmeFile = r.findFile("README.md").get
    readmeFile.content.contains(replacementText) should be(true)
  }

  it should "merge" is pending

  it should "return default children" in {
    val project = JavaClassTypeUsageTest.NewSpringBootProject
    val pmv = new ProjectMutableView(backingTemplates, project)
    val kids = pmv.defaultChildViews
    kids.nonEmpty should be (true)
    kids.forall(f => f.isInstanceOf[ArtifactContainerMutableView[_]]) should be (true)
  }

  it should "handle path and content replace" in {
    val project = JavaClassTypeUsageTest.NewSpringBootProject
    val pmv = new ProjectMutableView(backingTemplates, project)
    val oldPackage = "com.atomist.test1"
    val newPackage = "com.foo.bar"
    pmv.replace(oldPackage, newPackage)
    pmv.replaceInPath(oldPackage.replace(".", "/"), newPackage.replace(".", "/"))
    pmv.dirty should be(true)
    val r = pmv.currentBackingObject
    val pingPath = s"src/main/java/${newPackage.replace(".", "/")}/PingController.java"
    val pinger = r.findFile(pingPath).get
    pinger.content.contains("package " + newPackage) should be(true)
    val testPath = s"src/test/java/${newPackage.replace(".", "/")}/Test1OutOfContainerIntegrationTests.java"
    val test = r.findFile(testPath).get
    test.content.contains("package " + newPackage) should be(true)
  }

  it should "mergeTemplates to output root" in {
    val outputAs = EmptyArtifactSource("")
    val newContentDir = "newContent"
    val pmv = new ProjectMutableView(backingTemplates, outputAs)
    val ic = SimpleFunctionInvocationContext[ProjectMutableView]("project", null, pmv, outputAs, null,
      FirstPoa.parameterValues.map {
        case pv => (pv.getName, pv.getValue)
      }.toMap,
      FirstPoa, Nil)
    pmv.mergeTemplates("", newContentDir, ic)
    pmv.currentBackingObject.totalFileCount should be(3)

    val expectedPath = newContentDir + "/location_was_true.txt"
    // First.mustache
    pmv.currentBackingObject.findFile(newContentDir + "/" + static1.path).get.content should equal(static1.content)
    pmv.currentBackingObject.findFile(expectedPath).get.content should equal(FirstExpected)
    pmv.dirty should be(true)
  }

  it should "count files in a diectory" in {
    val src1 = StringFileArtifact("src/thing", "under src")
    val src2 = StringFileArtifact("src/main/otherThing", "under src/main")
    val asToEdit = SimpleFileBasedArtifactSource(src1, src2)
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), asToEdit)
    pmv.countFilesInDirectory("src") should be (1)
    pmv.countFilesInDirectory("xxx") should be (0)
  }

  it should "copy files under dir preserving path" in {
    val outputAs = EmptyArtifactSource("")
    val src1 = StringFileArtifact("src/thing", "under src")
    val src2 = StringFileArtifact("src/main/otherThing", "under src/main")
    val backingAs = new SimpleFileBasedArtifactSource("", Seq(
      StringFileArtifact("README.md", "in the root"),
      src1,
      src2,
      StringFileArtifact("other", "random")
    ))
    val pmv = new ProjectMutableView(backingAs, outputAs)
    pmv.copyEditorBackingFilesPreservingPath("src")
    pmv.currentBackingObject.totalFileCount should be(2)

    pmv.currentBackingObject.findFile(src1.path).get.content should equal(src1.content)
    pmv.currentBackingObject.findFile(src2.path).get.content should equal(src2.content)
    pmv.dirty should be(true)
  }

  it should "copy files under dir preserving path should respect children" in {
    val alreadyThere = StringFileArtifact("src/test.txt", "already there")
    val outputAs = new SimpleFileBasedArtifactSource("", alreadyThere)
    val src1 = StringFileArtifact("src/thing", "under src")
    val src2 = StringFileArtifact("src/main/otherThing", "under src/main")
    val backingAs = new SimpleFileBasedArtifactSource("", Seq(
      StringFileArtifact("README.md", "in the root"),
      src1,
      src2,
      StringFileArtifact("other", "random")
    ))
    val pmv = new ProjectMutableView(backingAs, outputAs)
    pmv.copyEditorBackingFilesPreservingPath("src")
    pmv.currentBackingObject.findFile(alreadyThere.path).get should equal (alreadyThere)

    pmv.currentBackingObject.totalFileCount should be(3)

    pmv.currentBackingObject.findFile(src1.path).get.content should equal(src1.content)
    pmv.currentBackingObject.findFile(src2.path).get.content should equal(src2.content)
    pmv.dirty should be(true)
  }

  it should "copy files under dir preserving path should not allow file specification" in {
    val alreadyThere = StringFileArtifact("src/test.txt", "already there")
    val outputAs = new SimpleFileBasedArtifactSource("", alreadyThere)
    val src1 = StringFileArtifact("src/thing", "under src")
    val src2 = StringFileArtifact("src/main/otherThing", "under src/main")
    val backingAs = new SimpleFileBasedArtifactSource("", Seq(
      StringFileArtifact("README.md", "in the root"),
      src1,
      src2,
      StringFileArtifact("other", "random")
    ))
    val pmv = new ProjectMutableView(backingAs, outputAs)
    an[InstantEditorFailureException] should be thrownBy pmv.copyEditorBackingFilesPreservingPath("src/thing")
  }

  it should "copy files under dir with new path" in {
    val outputAs = EmptyArtifactSource("")
    val newContentDir = "newContent"
    val src1 = StringFileArtifact("src/thing", "under src")
    val src2 = StringFileArtifact("src/main/otherThing", "under src/main")
    val backingAs = new SimpleFileBasedArtifactSource("", Seq(
      StringFileArtifact("README.md", "in the root"),
      src1,
      src2,
      StringFileArtifact("other", "random")
    ))
    val pmv = new ProjectMutableView(backingAs, outputAs)
    pmv.copyEditorBackingFilesWithNewRelativePath("src", newContentDir)
    pmv.currentBackingObject.totalFileCount should be(2)
    pmv.currentBackingObject.findFile(s"$newContentDir/thing").get.content should equal(src1.content)
    pmv.currentBackingObject.findFile(s"$newContentDir/main/otherThing").get.content should equal(src2.content)
    pmv.dirty should be(true)
  }

  it should "handle deleting of a directory" in {
    val project = JavaClassTypeUsageTest.NewSpringBootProject
    val pmv = new ProjectMutableView(backingTemplates, project)
    val oldPackage = "com.atomist.test1"
    val newPackage = "com.foo.bar"

    val gitDirectoryPath = s"dirToDelete"

    pmv.directoryExists(gitDirectoryPath) should be(true)

    pmv.dirty should be(false)

    pmv.deleteDirectory(gitDirectoryPath)

    pmv.directoryExists(gitDirectoryPath) should be(false)

    pmv.dirty should be(true)
  }

  it should "handle deleting of a file" in {
    val project = JavaClassTypeUsageTest.NewSpringBootProject
    val pmv = new ProjectMutableView(backingTemplates, project)

    val fileToDelete = "src/main/resources/application.properties"

    pmv.fileExists(fileToDelete) should be(true)

    pmv.dirty should be(false)

    pmv.deleteFile(fileToDelete)

    pmv.fileExists(fileToDelete) should be(false)

    pmv.dirty should be(true)
  }

  it should "handle copying a file" in {
    val project = JavaClassTypeUsageTest.NewSpringBootProject
    val pmv = new ProjectMutableView(backingTemplates, project)

    val src = "pom.xml"
    val dest = "foobar/pom2.xml"

    pmv.copyFile(src, dest)
    pmv.dirty should be (true)
    pmv.currentBackingObject.findFile(dest).get.content should equal (pmv.currentBackingObject.findFile(src).get.content)
  }

  it should "handle creating a directory" in {
    val project = JavaClassTypeUsageTest.NewSpringBootProject
    val pmv = new ProjectMutableView(backingTemplates, project)

    val directoryToCreate = "static"
    val pathToDirectory = "src/main/resources"
    val pathAndDirectoryName = pathToDirectory + "/" + directoryToCreate

    pmv.directoryExists(pathAndDirectoryName) should be(false)

    pmv.dirty should be(false)

    pmv.addDirectory(directoryToCreate, pathToDirectory)

    pmv.directoryExists(pathAndDirectoryName) should be(true)

    pmv.dirty should be(true)
  }

  it should "create directory and intermediate directories if not present" in {
    val project = JavaClassTypeUsageTest.NewSpringBootProject
    val pmv = new ProjectMutableView(backingTemplates, project)

    val directoryAndIntermdiateDirectoriesToCreate = "src/main/resources/parent/static/stuff"

    pmv.directoryExists(directoryAndIntermdiateDirectoriesToCreate) should be(false)

    pmv.dirty should be(false)

    pmv.addDirectoryAndIntermediates(directoryAndIntermdiateDirectoriesToCreate)

    pmv.directoryExists(directoryAndIntermdiateDirectoriesToCreate) should be(true)

    pmv.dirty should be(true)
  }

  it should "handle empty directory name and path" in {
    val project = JavaClassTypeUsageTest.NewSpringBootProject
    val pmv = new ProjectMutableView(backingTemplates, project)
    val directoryAndIntermdiateDirectoriesToCreate = ""
    pmv.directoryExists(directoryAndIntermdiateDirectoriesToCreate) should be(false)
    pmv.dirty should be(false)
    pmv.addDirectoryAndIntermediates(directoryAndIntermdiateDirectoriesToCreate)
    pmv.directoryExists(directoryAndIntermdiateDirectoriesToCreate) should be(true)
    pmv.dirty should be(true)
  }

  it should "move a file so that it's not found at its former address" in
    moveAFileAndVerifyNotFoundAtFormerAddress(pmv => ())

  it should "move a file so that it's not found at its former address even after adding another file" in
    moveAFileAndVerifyNotFoundAtFormerAddress(pmv => {
      pmv.addFile("some/path", "This is what I want you to put in. Am I Wrong??")
    })

  private def moveAFileAndVerifyNotFoundAtFormerAddress(stuffToDoLater: ProjectMutableView => Unit) = {
    val project = JavaClassTypeUsageTest.NewSpringBootProject
    val pmv = new ProjectMutableView(backingTemplates, project)
    val fmv = pmv.files.head
    val oldPath = fmv.path
    fmv.dirty should be (false)
    val newPath = "foobar/name"
    fmv.setPath(newPath)
    stuffToDoLater(pmv)
    fmv.dirty should be (true)
    fmv.path should equal (newPath)
    fmv.currentBackingObject.path should equal (newPath)
    pmv.files.map(_.path).contains(oldPath) should be (false)
    pmv.files.map(_.path).contains(newPath) should be (true)
  }

}
