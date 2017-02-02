package com.atomist.rug.kind.python3

import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.source.{EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.TreeNode
import com.atomist.tree.content.text.MutableContainerTreeNode
import com.atomist.tree.pathexpression.{PathExpressionEngine, PathExpressionParser}
import org.scalatest.{FlatSpec, Matchers}

class RawPython3Test extends FlatSpec with Matchers {

  import Python3ParserTest._

  private val pex = new PathExpressionEngine

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
    val expr = "/src/File()/PythonFile()//import_stmt()"
    val rtn = pex.evaluate(pmv, PathExpressionParser.parseString(expr), DefaultTypeRegistry)
    rtn.right.get.size should be>(2)
    rtn.right.get.foreach {
      case n: TreeNode if n.value.nonEmpty =>
      case x => //println(s"Was empty: $x")
    }
  }

  it should "drill down to Python import from statement using path expression" in {
    val proj = SimpleFileBasedArtifactSource(StringFileArtifact("src/setup.py", setupDotPy))
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr = "/src/File()/PythonFile()//import_from()"
    val rtn = pex.evaluate(pmv, PathExpressionParser.parseString(expr), DefaultTypeRegistry)
    rtn.right.get.size should be>(2)
    rtn.right.get.foreach {
      case n: TreeNode if n.value.nonEmpty =>
      case x => //println(s"Was empty: $x")
    }
  }
}
