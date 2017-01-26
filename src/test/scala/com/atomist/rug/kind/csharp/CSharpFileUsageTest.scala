package com.atomist.rug.kind.csharp

import org.scalatest.{FlatSpec, Matchers}

class CSharpFileUsageTest extends FlatSpec with Matchers {

  /*
  val Simple: ArtifactSource = new SimpleFileBasedArtifactSource("name",
    Seq(
      StringFileArtifact("setup.py", setupDotPy)
    ))

  val Flask1: ArtifactSource = Simple + new SimpleFileBasedArtifactSource("name",
    Seq(
      StringFileArtifact("hello.py", flask1)
    ))

  def execute(tsFilename: String, as: ArtifactSource, params: Map[String, String] = Map()): ModificationAttempt = {
    val pe = TestUtils.editorInSideFile(this, tsFilename)
    pe.modify(as, SimpleProjectOperationArguments("", params))
  }

  import PythonFileType._

  def modifyPythonAndReparseSuccessfully(tsFilename: String, as: ArtifactSource, params: Map[String, String] = Map()): ArtifactSource = {
    val parser = new Python3Parser
    execute(tsFilename, as, params) match {
      case sm: SuccessfulModification =>
        sm.result.allFiles
          .filter(_.name.endsWith(PythonExtension))
          .map(py => parser.parse(py.content))
          .map(tree => tree.childNodes.nonEmpty)
        sm.result
    }
  }

  it should "enumerate imports in simple project" in {
    val r = execute("ListImports.ts", Flask1)
    r match {
      case nmn: NoModificationNeeded =>
      case sm: SuccessfulModification =>
        val f = sm.result.findFile("setup.py").get
        fail
    }
  }

  it should "modify imports in single file" in {
    val r = modifyPythonAndReparseSuccessfully("ChangeImports.ts", Flask1)
    val f = r.findFile("hello.py").get
    f.content.contains("newImport") should be(true)
  }
  */

}
