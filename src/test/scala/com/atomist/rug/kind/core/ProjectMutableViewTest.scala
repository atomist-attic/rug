package com.atomist.rug.kind.core

import com.atomist.param.{ParameterValues, SimpleParameterValues, Tag}
import com.atomist.parse.java.ParsingTargets
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.project.edit.{Applicability, ModificationAttempt, ProjectEditor}
import com.atomist.rug.{EditorNotFoundException, SimpleRugResolver}
import com.atomist.rug.kind.java.JavaTypeUsageTest
import com.atomist.rug.spi.InstantEditorFailureException
import com.atomist.rug.ts.TypeScriptBuilder
import com.atomist.source._
import com.atomist.source.file.FileSystemArtifactSource
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConverters._

class ProjectMutableViewTest extends FlatSpec with Matchers {

  val SimpleEditor: String =
    s"""
       |import {Project} from '@atomist/rug/model/Core'
       |import {ProjectEditor} from '@atomist/rug/operations/ProjectEditor'
       |import {File} from '@atomist/rug/model/Core'
       |import {Parameter} from '@atomist/rug/operations/RugOperation'
       |
       |class SimpleEditor implements ProjectEditor {
       |    name: string = "Simple"
       |    description: string = "A nice little editor"
       |    edit(project: Project) {}
       |  }
       |export let editor = new SimpleEditor()
    """.stripMargin

  val atomistConfig: AtomistConfig = DefaultAtomistConfig

  val First =
    """Hello {{name}}
      |You have just won {{value}} dollars!
      |{{#in_ca}}
      |Well, {{taxed_value}} dollars, after taxes.{{/in_ca}}""".stripMargin

  val FirstPoa = SimpleParameterValues(Map[String, String](
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
    val project = JavaTypeUsageTest.NewSpringBootProject
    val pmv = new ProjectMutableView(backingTemplates, project)
    val regexToReplace = "To run locally"
    val replacementText = "To run somewhere else..."
    pmv.regexpReplace(regexToReplace, replacementText)
    assert(pmv.dirty === true)
    val r = pmv.currentBackingObject
    val readmeFile = r.findFile("README.md").get
    readmeFile.content.contains(replacementText) should be(true)
  }

  it should "correctly handle a regexReplace that has been accidentally specified with an @regex lookup" in {
    val project = JavaTypeUsageTest.NewSpringBootProject
    val pmv = new ProjectMutableView(backingTemplates, project)
    val regexToReplace = "@project_name"
    val replacementText = "To run somewhere else on @ symbol..."
    pmv.regexpReplace(regexToReplace, replacementText)
    assert(pmv.dirty === true)
    val r = pmv.currentBackingObject
    val readmeFile = r.findFile("README.md").get
    readmeFile.content.contains(replacementText) should be(true)
  }

  it should "merge" is pending

  it should "get simple project name with file system backed artifact" in {
    val as = ParsingTargets.NewStartSpringIoProject
    as.isInstanceOf[FileSystemArtifactSource] should be(true)
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), as)
    assert(pmv.name === "demo")
  }

  it should "correctly calculate totalFileCount" in {
    val as = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(as)
    assert(pmv.totalFileCount === as.totalFileCount)
  }

  it should "throw MissingEditorException if an editor cannot be found via editWith" in {
    val as = ParsingTargets.NewStartSpringIoProject
    as.isInstanceOf[FileSystemArtifactSource] should be(true)
    val tsf = StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleEditor)
    val otherAs = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(tsf))
    val resolver = SimpleRugResolver(otherAs)
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), as, DefaultAtomistConfig, Some(resolver.resolvedDependencies.resolvedRugs.head), rugResolver = Some(resolver))
    assertThrows[EditorNotFoundException] {
      pmv.editWith("blah", None)
    }
  }

  it should "throw EditorNotFoundException with a helpful message if we can help" in {
    val as = ParsingTargets.NewStartSpringIoProject
    as.isInstanceOf[FileSystemArtifactSource] should be(true)
    val tsf = StringFileArtifact(s".atomist/editors/SimpleEditor.ts", SimpleEditor)
    val otherAs = TypeScriptBuilder.compileWithModel(SimpleFileBasedArtifactSource(tsf))
    val resolver = SimpleRugResolver(otherAs)
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), as, DefaultAtomistConfig, Some(resolver.resolvedDependencies.resolvedRugs.head), rugResolver = Some(resolver))
    val caught = intercept[EditorNotFoundException] {
      pmv.editWith("fully:qualified:Simple", None)
    }
    assert(caught.getMessage === "Could not find editor: fully:qualified:Simple. Did you mean: Simple?")
  }

  it should "handle path and content replace" in {
    val project = JavaTypeUsageTest.NewSpringBootProject
    val pmv = new ProjectMutableView(backingTemplates, project)
    val oldPackage = "com.atomist.test1"
    val newPackage = "com.foo.bar"
    pmv.replace(oldPackage, newPackage)
    pmv.replaceInPath(oldPackage.replace(".", "/"), newPackage.replace(".", "/"))
    assert(pmv.dirty === true)
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
    pmv.mergeTemplates("", newContentDir, FirstPoa.parameterValues.map(pv => (pv.getName, pv.getValue)).toMap)
    assert(pmv.currentBackingObject.totalFileCount === 3)

    val expectedPath = newContentDir + "/location_was_true.txt"
    // First.mustache
    assert(pmv.currentBackingObject.findFile(newContentDir + "/" + static1.path).get.content === static1.content)
    assert(pmv.currentBackingObject.findFile(expectedPath).get.content === FirstExpected)
    assert(pmv.dirty === true)
    pmv.changeLogEntries should be(empty)
    assert(pmv.changeCount === 1)
  }

  // Written in response to community report of failure to preserve changes following this:
  //  with Project p
  //    begin
  //  do merge 'my_template.vm' to 'my_template.output'
  //  do copyEditorBackingFilesWithNewRelativePath sourcePath='test/' destinationPath='test_out'
  //  end
  it should "preserve merge after copyEditorBackingFilesWithNewRelativePath" in {
    val outputAs = EmptyArtifactSource("")
    val templatePath = "my_template.vm"
    val mergeOutputPath = "my_template.output"
    val copyInputFile = "foo"
    val copyInputDir = "test"
    val copyInputPath = s"$copyInputDir/$copyInputFile"
    val copyOutputDir = "test_out"
    val copyOutputPath = s"$copyOutputDir/$copyInputFile"
    val backing = SimpleFileBasedArtifactSource(
      StringFileArtifact(".atomist/templates/" + templatePath, "content"),
      StringFileArtifact(copyInputPath, "file content")
    )
    val pmv = new ProjectMutableView(backing, outputAs)
    //    val ic = SimpleFunctionInvocationContext[ProjectMutableView]("project", null, pmv, outputAs, null,
    //      FirstPoa.parameterValues.map(pv => (pv.getName, pv.getValue)).toMap,
    //      FirstPoa, Nil)
    val ic = FirstPoa.parameterValues.map(pv => (pv.getName, pv.getValue)).toMap
    pmv.merge(templatePath, mergeOutputPath, ic)
    assert(pmv.currentBackingObject.totalFileCount === 1)
    assert(pmv.currentBackingObject.cachedDeltas.size === 1)
    assert(pmv.currentBackingObject.findFile(mergeOutputPath).get.content === "content")
    pmv.currentBackingObject.cachedDeltas.exists(d => d.path == mergeOutputPath) should be(true)

    pmv.copyEditorBackingFilesWithNewRelativePath(sourceDir = copyInputDir, destinationPath = copyOutputDir)
    assert(pmv.totalFileCount === 2)
    assert(pmv.currentBackingObject.cachedDeltas.size === 3)
    assert(pmv.currentBackingObject.findFile(mergeOutputPath).get.content === "content")
    assert(pmv.currentBackingObject.findFile(copyOutputPath).get.content === "file content")
    pmv.currentBackingObject.cachedDeltas.exists(_.path == mergeOutputPath) should be(true)
    pmv.currentBackingObject.cachedDeltas.exists(_.path == copyOutputDir) should be(true)
    pmv.currentBackingObject.cachedDeltas.exists(_.path == copyOutputPath) should be(true)
  }

  it should "add two entries to change log" in {
    val outputAs = EmptyArtifactSource("")
    val pmv = new ProjectMutableView(backingTemplates, outputAs)
    pmv.addFile("src/main/whitespace", "      \t\n    \t")
    pmv.describeChange("Added valid program in Whitespace(tm) programming language")
    pmv.addFile("src/main/emoji", "\uD83E\uDD17")
    pmv.describeChange("Added valid program in emoji programming language, which will be exclusively " +
      "used by everyone born after 2005")
    assert(pmv.dirty === true)
    assert(pmv.changeLogEntries.size === 2)
    assert(pmv.changeCount === 2)
  }

  it should "count files in a directory" in {
    val src1 = StringFileArtifact("src/thing", "under src")
    val src2 = StringFileArtifact("src/main/otherThing", "under src/main")
    val asToEdit = SimpleFileBasedArtifactSource(src1, src2)
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), asToEdit)
    pmv.countFilesInDirectory("src") should be(1)
    pmv.countFilesInDirectory("xxx") should be(0)
    assert(pmv.changeCount === 0)
    assert(pmv.dirty === false)
    assert(pmv.changeLogEntries.isEmpty === true)
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
    assert(pmv.currentBackingObject.totalFileCount === 2)
    assert(pmv.currentBackingObject.findFile(src1.path).get.content === src1.content)
    assert(pmv.currentBackingObject.findFile(src2.path).get.content === src2.content)
    assert(pmv.dirty === true)
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
    assert(pmv.currentBackingObject.findFile(alreadyThere.path).get === alreadyThere)

    assert(pmv.currentBackingObject.totalFileCount === 3)

    assert(pmv.currentBackingObject.findFile(src1.path).get.content === src1.content)
    assert(pmv.currentBackingObject.findFile(src2.path).get.content === src2.content)
    assert(pmv.dirty === true)
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
    assert(pmv.currentBackingObject.totalFileCount === 2)
    assert(pmv.currentBackingObject.findFile(s"$newContentDir/thing").get.content === src1.content)
    assert(pmv.currentBackingObject.findFile(s"$newContentDir/main/otherThing").get.content === src2.content)
    assert(pmv.dirty === true)
  }

  it should "handle deleting of a directory" in {
    val project = JavaTypeUsageTest.NewSpringBootProject
    val pmv = new ProjectMutableView(backingTemplates, project)

    val gitDirectoryPath = s"dirToDelete"
    pmv.directoryExists(gitDirectoryPath) should be(true)
    assert(pmv.dirty === false)
    pmv.deleteDirectory(gitDirectoryPath)
    pmv.directoryExists(gitDirectoryPath) should be(false)
    assert(pmv.dirty === true)
  }

  it should "handle deleting of a file" in {
    val project = JavaTypeUsageTest.NewSpringBootProject
    val pmv = new ProjectMutableView(backingTemplates, project)

    val fileToDelete = "src/main/resources/application.properties"
    pmv.fileExists(fileToDelete) should be(true)
    assert(pmv.dirty === false)
    pmv.deleteFile(fileToDelete)
    pmv.fileExists(fileToDelete) should be(false)
    assert(pmv.dirty === true)
  }

  it should "handle copying a file" in {
    val project = JavaTypeUsageTest.NewSpringBootProject
    val pmv = new ProjectMutableView(backingTemplates, project)

    val src = "pom.xml"
    val dest = "foobar/pom2.xml"
    pmv.copyFile(src, dest)
    assert(pmv.dirty === true)
    assert(pmv.currentBackingObject.findFile(dest).get.content === pmv.currentBackingObject.findFile(src).get.content)
  }

  it should "handle creating a directory" in {
    val project = JavaTypeUsageTest.NewSpringBootProject
    val pmv = new ProjectMutableView(backingTemplates, project)

    val directoryToCreate = "static"
    val pathToDirectory = "src/main/resources"
    val pathAndDirectoryName = pathToDirectory + "/" + directoryToCreate
    pmv.directoryExists(pathAndDirectoryName) should be(false)
    assert(pmv.dirty === false)
    pmv.addDirectory(directoryToCreate, pathToDirectory)
    pmv.directoryExists(pathAndDirectoryName) should be(true)
    assert(pmv.dirty === true)
  }

  it should "create directory and intermediate directories if not present" in {
    val project = JavaTypeUsageTest.NewSpringBootProject
    val pmv = new ProjectMutableView(backingTemplates, project)

    val directoryAndIntermdiateDirectoriesToCreate = "src/main/resources/parent/static/stuff"
    pmv.directoryExists(directoryAndIntermdiateDirectoriesToCreate) should be(false)
    assert(pmv.dirty === false)
    pmv.addDirectoryAndIntermediates(directoryAndIntermdiateDirectoriesToCreate)
    pmv.directoryExists(directoryAndIntermdiateDirectoriesToCreate) should be(true)
    assert(pmv.dirty === true)
  }

  it should "handle empty directory name and path" in {
    val project = JavaTypeUsageTest.NewSpringBootProject
    val pmv = new ProjectMutableView(backingTemplates, project)
    val directoryAndIntermdiateDirectoriesToCreate = ""
    pmv.directoryExists(directoryAndIntermdiateDirectoriesToCreate) should be(false)
    assert(pmv.dirty === false)
    pmv.addDirectoryAndIntermediates(directoryAndIntermdiateDirectoriesToCreate)
    pmv.directoryExists(directoryAndIntermdiateDirectoriesToCreate) should be(true)
    assert(pmv.dirty === true)
  }

  it should "move a file so that it's not found at its former address" in
    moveAFileAndVerifyNotFoundAtFormerAddress(_ => ())

  it should "move a file so that it's not found at its former address even after adding another file" in
    moveAFileAndVerifyNotFoundAtFormerAddress(_.addFile("some/path", "This is what I want you to put in. Am I Wrong??"))

  it should "move an executable file so that it's not found at its former address even after adding another file" in
    moveAFileAndVerifyNotFoundAtFormerAddress(_.addExecutableFile("some/path", "This is what I want you to put in. Am I Wrong??"))

  it should "expose the backing archive as a PMV" in {
    val src1 = StringFileArtifact(".atomist/package.json", "{}}")
    val backingObject = SimpleFileBasedArtifactSource(src1)
    val pmv = new ProjectMutableView(backingObject, EmptyArtifactSource(""))
    pmv.countFilesInDirectory(".atomist") should be(0)
    val backingPMV = pmv.backingArchiveProject
    backingPMV.countFilesInDirectory(".atomist") should be(1)
    backingPMV.countFilesInDirectory("xxx") should be(0)
  }

  it should "make a file executable" in {
    val src1 = StringFileArtifact(".atomist/package.json", "{}}")
    val backingObject = SimpleFileBasedArtifactSource(src1)
    val pmv = new ProjectMutableView(backingObject, EmptyArtifactSource(""))
    pmv.countFilesInDirectory(".atomist") should be(0)
    val backingPMV = pmv.backingArchiveProject
    backingPMV.countFilesInDirectory(".atomist") should be(1)
    val file0 = backingPMV.findFile(".atomist/package.json")
    file0.currentBackingObject.mode should equal(FileArtifact.DefaultMode)
    
    backingPMV.makeExecutable(".atomist/package.json")
    backingPMV.countFilesInDirectory(".atomist") should be(1)
    val file1 = backingPMV.findFile(".atomist/package.json")
    file1.currentBackingObject.mode should equal(FileArtifact.ExecutableMode)
  }

  it should "return null on invalid file location request: no such type" in {
    val pmv = new ProjectMutableView(SimpleFileBasedArtifactSource(
      StringFileArtifact("x", "wopeiruowieuoriu")))
    assert(pmv.pathTo("x", "GastricBroodingFrog", 1, 1) === null)
  }

  it should "return null on invalid file location request: no such file" in {
    val pmv = new ProjectMutableView(EmptyArtifactSource(""))
    assert(pmv.pathTo("x", "Line", 1, 1) === null)
  }

  it should "not crash on location request: index out of bounds" in {
    val pmv = new ProjectMutableView(SimpleFileBasedArtifactSource(
      StringFileArtifact("Foo.java",
        """
          |public class Foo {
          |}
        """.stripMargin)
    ))
    pmv.pathTo("Foo.java", "JavaFile", -1, 10)
    pmv.pathTo("Foo.java", "JavaFile", 10000, 10)
  }

  it should "return file path on location request not in a structure" in {
    val pmv = new ProjectMutableView(SimpleFileBasedArtifactSource(
      StringFileArtifact("Foo.java",
        """
          |public class Foo {
          |}
        """.stripMargin)
    ))
    // This is just padding
    val path = pmv.pathTo("Foo.java", "JavaFile", 1, 1)
    assert(path == "/File()[@path='Foo.java']/JavaFile()")
  }

  it should "return non-null on valid file location request" in {
    val pmv = new ProjectMutableView(SimpleFileBasedArtifactSource(
      StringFileArtifact("Foo.java",
        """
          |public class Foo {
          |}
        """.stripMargin)
    ))
    val path = pmv.pathTo("Foo.java", "JavaFile", 1, 2)
    assert(path.contains("classDeclaration"))
  }

  private def moveAFileAndVerifyNotFoundAtFormerAddress(stuffToDoLater: ProjectMutableView => Unit) = {
    val project = JavaTypeUsageTest.NewSpringBootProject
    val pmv = new ProjectMutableView(backingTemplates, project)
    val fmv = pmv.files.asScala.head
    val oldPath = fmv.path
    assert(fmv.dirty === false)
    val newPath = "foobar/name"
    fmv.setPath(newPath)
    stuffToDoLater(pmv)
    assert(fmv.dirty === true)
    assert(fmv.path === newPath)
    assert(fmv.currentBackingObject.path === newPath)
    pmv.files.asScala.map(_.path).contains(oldPath) should be(false)
    pmv.files.asScala.map(_.path).contains(newPath) should be(true)
  }
}
