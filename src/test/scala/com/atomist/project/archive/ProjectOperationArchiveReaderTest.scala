package com.atomist.project.archive

import java.io.File

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.rug.Import
import com.atomist.rug.runtime.TypeScriptRugEditorTest
import com.atomist.source.file.ClassPathArtifactSource
import com.atomist.source.file.ClassPathArtifactSource._
import com.atomist.source.{ArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConversions._

class ProjectOperationArchiveReaderTest extends FlatSpec with Matchers {

  val atomistConfig: AtomistConfig = DefaultAtomistConfig

  val FirstEditor = StringFileArtifact(atomistConfig.editorsRoot + "/First.rug",
    """
      |editor First
      |
      |with file f do setPath ""
    """.stripMargin
  )

  val SecondOp = StringFileArtifact(atomistConfig.reviewersRoot + "/Second.rug",
    """
      |reviewer Second
      |
      |with file f do setPath ""
    """.stripMargin
  )

  val Generator = StringFileArtifact(atomistConfig.editorsRoot + "/Published.rug",
    """
      |@generator
      |editor Published
      |
      |First
    """.stripMargin
  )

  val EditorWithImports = StringFileArtifact(atomistConfig.editorsRoot + "/EditorWithImports.rug",
    """
      |editor EditorWithImports
      |
      |uses atomist.common-editors.UpdateReadme
      |uses atomist.common-editors.PomParameterizer
      |
      |with file f do setPath ""
    """.stripMargin
  )

  it should "parse single editor" in {
    val apc = new ProjectOperationArchiveReader(atomistConfig)
    val ops = apc.findOperations(new SimpleFileBasedArtifactSource("", FirstEditor), None, Nil)
    ops.editors.size should be(1)
    ops.editorNames should equal(Seq("First"))
    ops.reviewers should equal(Nil)
    ops.reviewerNames should equal(Nil)
    ops.generators should equal(Nil)
    ops.generatorNames should equal(Nil)
  }

  it should "parse editor and reviewer" in {
    val apc = new ProjectOperationArchiveReader(atomistConfig)
    val ops = apc.findOperations(new SimpleFileBasedArtifactSource("", Seq(FirstEditor, SecondOp)), None, Nil)
    ops.editors.size should be(1)
    ops.editorNames should equal(Seq("First"))
    ops.reviewers.size should be(1)
    ops.reviewerNames should equal(Seq("Second"))
    ops.generators should equal(Nil)
    ops.generatorNames should equal(Nil)
  }

  it should "parse editor and reviewer and generator" in {
    val apc = new ProjectOperationArchiveReader(atomistConfig)
    val ops = apc.findOperations(new SimpleFileBasedArtifactSource("", Seq(FirstEditor, SecondOp, Generator)), None, Nil)
    ops.editors.size should be(1)
    ops.editorNames.toSet should equal(Set("First"))
    ops.reviewers.size should be(1)
    ops.reviewerNames should equal(Seq("Second"))
    ops.generators.size should equal(1)
    ops.generatorNames should equal(Seq("Published"))
  }

  it should "find imports in a project that uses them" in {
    val apc = new ProjectOperationArchiveReader(atomistConfig)
    val imports = apc.findImports(new SimpleFileBasedArtifactSource("", Seq(FirstEditor, SecondOp, Generator, EditorWithImports)))
    imports should equal(Seq(
      Import("atomist.common-editors.UpdateReadme"),
      Import("atomist.common-editors.PomParameterizer")
    ))
  }

  it should "find no imports in a project that uses none" in {
    val apc = new ProjectOperationArchiveReader(atomistConfig)
    val imports = apc.findImports(new SimpleFileBasedArtifactSource("", Seq(FirstEditor, SecondOp, Generator)))
    imports should equal(Nil)
  }

  it should "find typescript editor" in {
    val apc = new ProjectOperationArchiveReader(atomistConfig)
    val as = SimpleFileBasedArtifactSource(
      StringFileArtifact(".atomist/editors/SimpleEditor.ts",
        TypeScriptRugEditorTest.SimpleEditorTaggedAndMeta)
    )
    val ops = apc.findOperations(as, None, Nil)
    ops.editors.size should be(1)
    ops.editors.head.parameters.size should be(1)
  }

  it should "find and invoke typescript generator" in {
    val apc = new ProjectOperationArchiveReader(atomistConfig)
    val f1 = StringFileArtifact("package.json", "{}")
    val f2 = StringFileArtifact("app/Thing.ts", "class Thing {}")
    val rugAs = SimpleFileBasedArtifactSource(
      StringFileArtifact(".atomist/editors/SimpleGenerator.ts",
        TypeScriptRugEditorTest.SimpleGenerator),
      f1,
      f2
    )
    val ops = apc.findOperations(rugAs, None, Nil)
    ops.generators.size should be(1)
    ops.generators.head.parameters.size should be(0)
    val result = ops.generators.head.generate(SimpleProjectOperationArguments.Empty)
    // Should preserve content from the backing archive
    result.findFile(f1.path).get.content.equals(f1.content) should be(true)
    result.findFile(f2.path).get.content.equals(f2.content) should be(true)

    // Should contain new contain
    result.findFile("src/from/typescript").get.content.contains("Anders") should be(true)
  }
}
