package com.atomist.project.archive

import com.atomist.rug.Import
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class RugArchiveReaderTest extends FlatSpec with Matchers {

  val atomistConfig: AtomistConfig = DefaultAtomistConfig

  val FirstEditor = StringFileArtifact(atomistConfig.editorsRoot + "/First.rug",
    """
      |editor First
      |
      |with File f do setPath ""
    """.stripMargin
  )

  val SecondOp = StringFileArtifact(atomistConfig.reviewersRoot + "/Second.rug",
    """
      |reviewer Second
      |
      |with File f do setPath ""
    """.stripMargin
  )

  val Generator = StringFileArtifact(atomistConfig.editorsRoot + "/Published.rug",
    """
      |generator Published
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
      |with File f do setPath ""
    """.stripMargin
  )

  val EditorWithProjectName = StringFileArtifact(atomistConfig.editorsRoot + "/Stuff.rug",
    """
      |editor Stuff
      |
      |param project_name: ^.*$
      |
      |with File f do setPath ""
    """.stripMargin
  )

  val AnotherEditorWithProjectName = StringFileArtifact(atomistConfig.editorsRoot + "/MoreStuff.rug",
    """
      |editor MoreStuff
      |
      |param project_name: ^.*$
      |
      |with File f do setPath ""
    """.stripMargin
  )

  val GeneratorWithoutProjectName = StringFileArtifact(atomistConfig.editorsRoot + "/Published.rug",
    """
      |generator Published
      |
      |uses Stuff
      |uses MoreStuff
      |
      |Stuff
      |MoreStuff
    """.stripMargin
  )

  //https://github.com/atomist/rug/issues/258
  it should "only describe a single project_name parameter if it's declared" in {
    val apc = new RugDslArchiveReader(atomistConfig)
    val ops = apc.find(new SimpleFileBasedArtifactSource("", Seq(GeneratorWithoutProjectName, EditorWithProjectName, AnotherEditorWithProjectName)), None, Nil)
    assert(ops.generators.size === 1)
    assert(ops.generators.head.parameters.size === 1)
    ops.toString.contains("Published") should be (true)
    ops.toString.contains("Stuff") should be (true)
  }

  it should "parse single editor" in {
    val apc = new RugDslArchiveReader(atomistConfig)
    val ops = apc.find(new SimpleFileBasedArtifactSource("", FirstEditor), None, Nil)
    assert(ops.editors.size === 1)
    assert(ops.editorNames === Seq("First"))
    assert(ops.reviewers === Nil)
    assert(ops.reviewerNames === Nil)
    assert(ops.generators === Nil)
    assert(ops.generatorNames === Nil)
  }

  it should "parse editor and reviewer" in {
    val apc = new RugDslArchiveReader(atomistConfig)
    val ops = apc.find(new SimpleFileBasedArtifactSource("", Seq(FirstEditor, SecondOp)), None, Nil)
    assert(ops.editors.size === 1)
    assert(ops.editorNames === Seq("First"))
    assert(ops.reviewers.size === 1)
    assert(ops.reviewerNames === Seq("Second"))
    assert(ops.generators === Nil)
    assert(ops.generatorNames === Nil)
  }

  it should "parse editor and reviewer and generator" in {
    val apc = new RugDslArchiveReader(atomistConfig)
    val ops = apc.find(new SimpleFileBasedArtifactSource("", Seq(FirstEditor, SecondOp, Generator)), None, Nil)
    assert(ops.editors.size === 1)
    assert(ops.editorNames.toSet === Set("First"))
    assert(ops.reviewers.size === 1)
    assert(ops.reviewerNames === Seq("Second"))
    assert(ops.generators.size === 1)
    assert(ops.generatorNames === Seq("Published"))
  }

  it should "find imports in a project that uses them" in {
    val apc = new RugDslArchiveReader(atomistConfig)
    val imports = apc.findImports(new SimpleFileBasedArtifactSource("", Seq(FirstEditor, SecondOp, Generator, EditorWithImports)))
    imports should equal(Seq(
      Import("atomist.common-editors.UpdateReadme"),
      Import("atomist.common-editors.PomParameterizer")
    ))
  }

  it should "find no imports in a project that uses none" in {
    val apc = new RugDslArchiveReader(atomistConfig)
    val imports = apc.findImports(new SimpleFileBasedArtifactSource("", Seq(FirstEditor, SecondOp, Generator)))
    imports should equal(Nil)
  }
}