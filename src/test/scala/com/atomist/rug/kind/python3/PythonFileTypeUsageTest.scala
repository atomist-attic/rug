package com.atomist.rug.kind.python3

import com.atomist.param.SimpleParameterValues
import com.atomist.project.edit.{ModificationAttempt, NoModificationNeeded, SuccessfulModification}
import com.atomist.rug.TestUtils
import com.atomist.rug.kind.grammar.AbstractTypeUnderFileTest
import com.atomist.source.{ArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}

class PythonFileTypeUsageTest extends AbstractTypeUnderFileTest {

  override protected val typeBeingTested = new PythonFileType

  import Python3ParserTest._

  val Simple: ArtifactSource = new SimpleFileBasedArtifactSource("name",
    Seq(
      StringFileArtifact("setup.py", setupDotPy)
    ))

  val Flask1: ArtifactSource = Simple + new SimpleFileBasedArtifactSource("name",
    Seq(
      StringFileArtifact("hello.py", flask1)
    ))

  def executePython(tsFilename: String, as: ArtifactSource, params: Map[String,String] = Map()): ModificationAttempt = {
    val pe = TestUtils.editorInSideFile(this, tsFilename)
    pe.modify(as, SimpleParameterValues( params))
  }

  it should "enumerate imports in simple project" in {
    val r = executePython("ListImports.ts", Flask1)
    r match {
      case nmn: NoModificationNeeded =>
      case sm: SuccessfulModification =>
        val f = sm.result.findFile("setup.py").get
        fail
      case _ => ???
    }
  }

  it should "modify imports in single file" in {
    val r = modifyAndReparseSuccessfully("ChangeImports.ts", Flask1)
    val f = r.findFile("hello.py").get
    f.content.contains("newImport") should be(true)
  }

  private val newRoute =
    """
      |@app.route("/")
      |def hello2():
      |    return "Hello World!"
    """.stripMargin

}
