package com.atomist.rug.kind.xml

import com.atomist.parse.java.ParsingTargets
import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.source.EmptyArtifactSource
import com.atomist.tree.pathexpression.{PathExpressionEngine, PathExpressionParser}
import com.atomist.tree.utils.TreeNodeUtils
import com.atomist.tree.{ContainerTreeNode, TreeNode}
import org.scalatest.{FlatSpec, Matchers}

class XmlFileTypeTest extends FlatSpec with Matchers {

  private val pex = new PathExpressionEngine

  it should "find XML file type using path expression" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr = "/File()[@name='pom.xml']/XmlFile()"
    val rtn = pex.evaluate(pmv, PathExpressionParser.parseString(expr), DefaultTypeRegistry)
    rtn.right.get.size should be(1)
    rtn.right.get.foreach {
      case n: ContainerTreeNode =>
        n.value should equal (proj.findFile("pom.xml").get.content)
    }
  }

  it should "drill down to named XML element using path expression with name" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr = "//XmlFile()//plugin"
    val rtn = pex.evaluate(pmv, PathExpressionParser.parseString(expr), DefaultTypeRegistry)
    rtn.right.get.size should be (1)
    rtn.right.get.foreach {
      case n: TreeNode if n.value.nonEmpty =>
        //println(n.value)
      case x => println(s"Was empty: $x")
    }
  }

  it should "drill down to named XML element using path expression with name predicate" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr = "//XmlFile()//element()[@name='plugin']"
    val rtn = pex.evaluate(pmv, PathExpressionParser.parseString(expr), DefaultTypeRegistry)
    rtn.right.get.size should be (1)
    rtn.right.get.foreach {
      case n: TreeNode if n.value.nonEmpty =>
        //println(n.value)
      case x => println(s"Was empty: $x")
    }
  }

  it should "drill down to named XML element" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr = "/*[@name='pom.xml']/XmlFile()/project/groupId" // Works with content
    val rtn = pex.evaluate(pmv, PathExpressionParser.parseString(expr), DefaultTypeRegistry)
    println(TreeNodeUtils.toShortString(rtn.right.get.head))

    rtn.right.get.size should be (1)
    rtn.right.get.foreach {
      case n: TreeNode if n.value.nonEmpty =>
      //println(n.value)
      case x => println(s"Was empty: $x")
    }
  }
}
