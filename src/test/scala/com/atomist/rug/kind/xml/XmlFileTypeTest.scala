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
        n.value should equal(proj.findFile("pom.xml").get.content)
    }
  }

  it should "drill down using names" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr = "//XmlFile()/project/dependencies/dependency"
    val rtn = pex.evaluate(pmv, PathExpressionParser.parseString(expr), DefaultTypeRegistry)
    rtn.right.get.size should be(2)
    rtn.right.get.foreach {
      case n: TreeNode if n.value.nonEmpty =>
      n.value.contains("org.springframework.boot") should be (true)
      case x => fail(s"Was empty: $x")
    }
  }

  it should "drill down to named XML elements using path expression with name" in
    drillToGroupIds("//XmlFile()//groupId")

  it should "drill down to named XML elements using path expression with name predicate" in
    drillToGroupIds("//XmlFile()//element()[@name='groupId']")

  it should "drill down to named XML elements using path expression and type //" in
    drillToGroupIds("//XmlFile()//element()", tn => tn.nodeName == "groupId")

  private def drillToGroupIds(expr: String, filter: TreeNode => Boolean = tn => true): Unit = {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val rtn = pex.evaluate(pmv, PathExpressionParser.parseString(expr), DefaultTypeRegistry)
    val results = rtn.right.get.filter(filter)
    results.size should be(5)
    // TODO note that we can't presently rely on ordering
//    println(results.map(TreeNodeUtils.toShortString))
//    println(results.map(n => n.asInstanceOf[MutableContainerMutableView].currentBackingObject.asInstanceOf[PositionedTreeNode].startPosition))
//    results.tail.head.value should be ("<groupId>org.springframework.boot</groupId>")
//    results.head.value should be ("<groupId>com.example</groupId>")
    results.exists(r => r.value == "<groupId>org.springframework.boot</groupId>") should be (true)
    results.exists(r => r.value == "<groupId>com.example</groupId>") should be (true)
  }

  it should "drill down to named XML element" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr = "/*[@name='pom.xml']/XmlFile()/project/groupId"
    val rtn = pex.evaluate(pmv, PathExpressionParser.parseString(expr), DefaultTypeRegistry)

    rtn.right.get.size should be(1)
    rtn.right.get.foreach {
      case n: TreeNode if n.value.nonEmpty =>
      //println(n.value)
      case x => fail(s"Was empty: $x")
    }
  }

  it should "double drill down to named XML element" in {
    val proj = ParsingTargets.NewStartSpringIoProject
    val pmv = new ProjectMutableView(EmptyArtifactSource(""), proj, DefaultAtomistConfig)
    val expr = "/*[@name='pom.xml']/XmlFile()/project/dependencies/dependency/scope//TEXT"
    val rtn = pex.evaluate(pmv, PathExpressionParser.parseString(expr), DefaultTypeRegistry)
    //println(TreeNodeUtils.toShortString(rtn.right.get.head))

    rtn.right.get.size should be(1)
    rtn.right.get.head.value should equal ("test")
  }

}
