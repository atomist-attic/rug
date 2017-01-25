package com.atomist.rug.kind.python3

import com.atomist.parse.java.ParsingTargets
import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.kind.java.JavaClassOrInterfaceView
import com.atomist.source.{EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.content.text.MutableContainerTreeNode
import com.atomist.tree.pathexpression.{PathExpressionEngine, PathExpressionParser}
import com.atomist.tree.utils.TreeNodeUtils
import org.scalatest.{FlatSpec, Matchers}

class Python3ParserTest extends FlatSpec with Matchers {

  private val pex = new PathExpressionEngine

  lazy val parser = new Python3Parser

  import Python3ParserTest._

  //  it should "parse simple file and find imports" in pendingUntilFixed {
  //    val parsed = parser.parse(setupDotPy)
  //    val imports = findByName("import_name", parsed)
  //
  //    imports.size should be (5)
  //  }

  def stringShowingIndices(s: String) = {
    val z = s.zipWithIndex
    "len=" + s.length + " :" + z.mkString(",")
  }

  it should "parse simplest file and write out unchanged" in {
    parseAndVerifyValueCanBeWrittenOutUnchanged(simplestDotPy)
  }

  it should "parse simplest file with initial newline and write out unchanged" in {
    parseAndVerifyValueCanBeWrittenOutUnchanged(simplestDotPyWithInitialNewLine)
  }

  it should "parse simple file and write out unchanged" in
    parseAndVerifyValueCanBeWrittenOutUnchanged(setupDotPy)

  it should "parse simple script and write out unchanged" in
    parseAndVerifyValueCanBeWrittenOutUnchanged(dateParserDotPy)

  it should "find Python file type using path expression" in {
    val proj = SimpleFileBasedArtifactSource(StringFileArtifact("src/setup.py", setupDotPy))
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr = "/src/File()/PythonFile()"
    val rtn = pex.evaluate(pmv, PathExpressionParser.parseString(expr), DefaultTypeRegistry)
    rtn.right.get.size should be(1)
//    rtn.right.get.foreach {
//      case p: PythonFileMutableView =>
//    }
  }

  it should "drill down to Python import statement using path expression" in {
    val proj = SimpleFileBasedArtifactSource(StringFileArtifact("src/setup.py", setupDotPy))
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr = "/src/File()/PythonRawFile()//import_stmt()"
    val rtn = pex.evaluate(pmv, PathExpressionParser.parseString(expr), DefaultTypeRegistry)
    rtn.right.get.size should be>(2)
    rtn.right.get.foreach {
      case n: MutableContainerTreeNode =>
        println(n.value)
      case x => println(x)
    }
  }

  private def parseAndVerifyValueCanBeWrittenOutUnchanged(pyProg: String): MutableContainerTreeNode = {
    if (pyProg.lines.exists(_.trim.startsWith("|")))
      fail(s"Probably a test error. Did you forget to call stripMargin?")
    val parsed = parser.parse(pyProg)

    val writtenOut = parsed.value
    val comp = s"Result: --------------\n[$writtenOut]\nExpected: -----------\n[$pyProg]"
    withClue(comp) {
      writtenOut should equal(pyProg)
    }
    parsed
  }

}

object Python3ParserTest {

  val simplestDotPy =
    """import os
      |import sys
    """.stripMargin

  val simplestDotPyWithInitialNewLine =
    """
      |import os
      |import sys
    """.stripMargin

  val setupDotPy =
    """
      |import os
      |import sys
      |try:
      |    from setuptools import setup
      |except ImportError:
      |    from distutils.core import setup
      |
      |sys.path.insert(0, '.')
      |from botletpy import __version__
      |
      |
      |setup(
      |    name="botletpy",
      |    version=__version__,
      |    description="Library serving as a brick to build botlets in Python.",
      |    maintainer="Atomist",
      |    maintainer_email="sylvain@atomist.com",
      |    packages=["botletpy"],
      |    platforms=["any"],
      |    long_description=open(os.path.join(os.path.dirname(__file__), 'README.md')).read()
      |)
    """.stripMargin

  val dateParserDotPy =
    """
      |from datetime import datetime
      |
      |now = datetime.now()
      |
      |mm = str(now.month)
      |
      |dd = str(now.day)
      |
      |yyyy = str(now.year)
      |
      |hour = str(now.hour)
      |
      |mi = str(now.minute)
      |
      |ss = str(now.second)
      |
      |print mm + "/" + dd + "/" + yyyy + " " + hour + ":" + mi + ":" + ss
      |
      """.stripMargin

  val flask1 =
    """
      |from flask import Flask
      |app = Flask(__name__)
      |
      |@app.route("/")
      |def hello():
      |    return "Hello World!"
      |
      |if __name__ == "__main__":
      |    app.run()
    """.stripMargin
}