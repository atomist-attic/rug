package com.atomist.rug.kind.python3

import com.atomist.project.SimpleProjectOperationArguments
import com.atomist.project.edit.{ModificationAttempt, NoModificationNeeded, ProjectEditor, SuccessfulModification}
import com.atomist.rug.DefaultRugPipeline
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.source.{ArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import org.scalatest.{FlatSpec, Matchers}

class PythonTypeUsageTest extends FlatSpec with Matchers {

  import Python3ParserTest._

  val Simple: ArtifactSource = new SimpleFileBasedArtifactSource("name",
    Seq(
      StringFileArtifact("setup.py", setupDotPy)
    ))

  val Flask1: ArtifactSource = new SimpleFileBasedArtifactSource("name",
    Seq(
      StringFileArtifact("hello.py", flask1)
    ))

  def executePython(program: String, as: ArtifactSource, params: Map[String,String] = Map()): ModificationAttempt = {
    val runtime = new DefaultRugPipeline(DefaultTypeRegistry)
    val eds = runtime.createFromString(program)
    eds.size should be(1)
    val pe = eds.head.asInstanceOf[ProjectEditor]
    pe.modify(as, SimpleProjectOperationArguments("", params))
  }

  import PythonType._

  def modifyPythonAndReparseSuccessfully(program: String, as: ArtifactSource, params: Map[String,String] = Map()): ArtifactSource = {
    val parser = new Python3Parser
    executePython(program, as, params) match {
      case sm: SuccessfulModification =>
        sm.result.allFiles
          .filter(_.name.endsWith(PythonExtension))
          .map(py => parser.parse(py.content))
          .map(tree => tree.childNodes.nonEmpty)
        sm.result
    }
  }

  it should "enumerate imports in simple file" in {
    val prog =
      """
        |editor ImportBrowser
        |
        |with python
        | with import imp
        |   do eval { print("From Rug!! " + imp) }
      """.stripMargin
    val r = executePython(prog, Simple)
    r match {
      case nmn: NoModificationNeeded =>
      case sm: SuccessfulModification =>
        val f = sm.result.findFile("setup.py").get
        fail
    }
  }

  it should "modify imports in simple file" in {
    val prog =
      """
        |editor ImportUpdater
        |
        |with python
        | with import
        |   do setName "newImport"
      """.stripMargin
    val r = modifyPythonAndReparseSuccessfully(prog, Simple)
    val f = r.findFile("setup.py").get
    f.content.contains("newImport") should be(true)
  }

  val newRoute =
    """
      |@app.route("/")
      |def hello2():
      |    return "Hello World!"
    """.stripMargin

  it should "add Flask route to Python file" in {
    val prog =
      """
        |editor AddFlaskRoute
        |
        |param new_route: ^[\s\S]*$
        |
        |with python when filename = "hello.py"
        | do append new_route
      """.stripMargin
    val r = modifyPythonAndReparseSuccessfully(prog, Flask1, Map(
      "new_route" -> newRoute
    ))
    val f = r.findFile("hello.py").get
    f.content.contains(newRoute) should be(true)

    val reparsed = pythonParser.parse(f.content)
    // TODO assert 2 methods
    // reparsed.f
  }

}
