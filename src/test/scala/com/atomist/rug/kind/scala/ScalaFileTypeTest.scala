package com.atomist.rug.kind.scala

import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.kind.dynamic.MutableContainerMutableView
import com.atomist.source.{EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.{MutableTreeNode, TreeNode}
import com.atomist.tree.content.text.{ConsoleMatchListener, TreeNodeOperations}
import com.atomist.tree.pathexpression.{ExpressionEngine, PathExpressionEngine, PathExpressionParser}
import com.atomist.tree.utils.TreeNodeUtils
import org.scalatest.{FlatSpec, Matchers}

class ScalaFileTypeTest extends FlatSpec with Matchers {

  import PathExpressionParser._
  import ScalaFileTypeTest._

  val ee: ExpressionEngine = new PathExpressionEngine

  val scalaType = new ScalaFileType

  it should "ignore ill-formed file without error" in {
    val scalas = scalaType.findAllIn(ProjectWithBogusScala)
    // Should have silently ignored the bogus file
    scalas.size should be(1)
  }

  it should "parse hello world" in {
    val scalas = scalaType.findAllIn(helloWorldProject)
    scalas.size should be(1)
  }

  it should "parse hello world and write out correctly" in {
    val parsed = scalaType.fileToRawNode(HelloWorldScala, Some(ConsoleMatchListener)).get
    val parsedValue = parsed.value
    withClue(s"Unexpected content: [$parsedValue]") {
      parsedValue should equal(HelloWorldScala.content)
    }
  }

  it should "parse hello world into mutable view and write out unchanged" in {
    val scalas = scalaType.findAllIn(helloWorldProject)
    scalas.size should be(1)
    scalas.head.head match {
      case mtn: MutableContainerMutableView =>
        val content = mtn.value
        content should equal(helloWorldProject.files.get(0).content)
    }
  }

  it should "find hello world using path expression" in {
    val expr = "//ScalaFile()"
    val rtn = ee.evaluate(helloWorldProject, expr, DefaultTypeRegistry)
    rtn.right.get.size should be(1)
  }

  it should "find specific exception catch" in {
    val scalas: Option[Seq[TreeNode]] = scalaType.findAllIn(exceptionsProject)
    scalas.size should be(1)
    val scalaFileNode = scalas.get.head.asInstanceOf[MutableContainerMutableView]

    val expr = "//termTryWithCases/case//typeName[@value='ThePlaneHasFlownIntoTheMountain']"
      ee.evaluate(scalaFileNode, expr, DefaultTypeRegistry) match {
        case Right(nodes) if nodes.nonEmpty =>
          nodes.size should be(1)
          nodes.head.value should be("ThePlaneHasFlownIntoTheMountain")
        case wtf => fail(s"Expression didn't match [$wtf]. The tree was " + TreeNodeUtils.toShorterString(scalaFileNode))
      }

    scalaFileNode.value should equal(Exceptions.content)
  }

  it should "find and modify specific exception catch" in {
    val proj = exceptionsProject
    val scalas: Option[Seq[TreeNode]] = scalaType.findAllIn(proj)
    scalas.size should be(1)
    val scalaFileNode = scalas.get.head.asInstanceOf[MutableContainerMutableView]

    val newException = "MicturationException"

    val expr = "//termTryWithCases/case//typeName[@value='ThePlaneHasFlownIntoTheMountain']"
    ee.evaluate(scalaFileNode, expr, DefaultTypeRegistry) match {
      case Right(nodes) if nodes.nonEmpty =>
        nodes.size should be(1)
        val mut = nodes.head.asInstanceOf[MutableContainerMutableView]
        mut.update(newException)
      case wtf =>
        fail(s"Expression didn't match [$wtf]. The tree was " + TreeNodeUtils.toShorterString(scalaFileNode))
    }

    val newContent = Exceptions.content.replaceFirst("ThePlaneHasFlownIntoTheMountain", newException)
    scalaFileNode.value should equal(newContent)

    scalaFileNode.dirty should be (true)
    val updatedFile = proj.findFile(Exceptions.path)
    updatedFile.content should be(newContent)
    //updatedFile.dirty should be(true)
  }

  it should "find and modify multiple points" in {
    val proj = exceptionsProject
    val scalas: Option[Seq[TreeNode]] = scalaType.findAllIn(proj)
    scalas.size should be(1)
    val scalaFileNode = scalas.get.head.asInstanceOf[MutableContainerMutableView]

    val newException = "MicturationException"

    val expr = "//case//typeName[@value='ThePlaneHasFlownIntoTheMountain']"
    ee.evaluate(scalaFileNode, expr, DefaultTypeRegistry) match {
      case Right(nodes) if nodes.nonEmpty =>
        nodes.size should be(2)
        nodes.foreach {
          case mut: MutableContainerMutableView => mut.update(newException)
          case _ =>
        }
      case wtf =>
        fail(s"Expression didn't match [$wtf]. The tree was " + TreeNodeUtils.toShorterString(scalaFileNode))
    }

    val newContent = Exceptions.content.replaceAll("ThePlaneHasFlownIntoTheMountain", newException)
    scalaFileNode.value should equal(newContent)

    scalaFileNode.dirty should be (true)
    val updatedFile = proj.findFile(Exceptions.path)
    updatedFile.content should be(newContent)
    //updatedFile.dirty should be(true)
  }

  it should "find and modify specific exception catch body" in {
    val proj = exceptionsProject
    val scalas: Option[Seq[TreeNode]] = scalaType.findAllIn(proj)
    scalas.size should be(1)
    val scalaFileNode = scalas.get.head.asInstanceOf[MutableContainerMutableView]

    val newException = "MicturationException"
    //println(TreeNodeUtils.toShorterString(scalaFileNode))

    val expr = "//termTryWithCases/case[//typeName[@value='ThePlaneHasFlownIntoTheMountain']]"
    ee.evaluate(scalaFileNode, expr, DefaultTypeRegistry) match {
      case Right(nodes) if nodes.nonEmpty =>
        nodes.size should be(1)
        val mut = nodes.head.asInstanceOf[MutableContainerMutableView]
        val terminals = TreeNodeOperations.terminals(mut)
        //println(terminals)
        terminals(1).asInstanceOf[MutableTreeNode].update(newException)
      case wtf =>
        fail(s"Expression didn't match [$wtf]. The tree was " + TreeNodeUtils.toShorterString(scalaFileNode))
    }

    val newContent = Exceptions.content.replaceFirst("ThePlaneHasFlownIntoTheMountain", newException)
    scalaFileNode.value should equal(newContent)

    scalaFileNode.dirty should be (true)
    val updatedFile = proj.findFile(Exceptions.path)
    updatedFile.content should be(newContent)
    //updatedFile.dirty should be(true)
  }

}

object ScalaFileTypeTest {

  val BogusScala = StringFileArtifact("Test.scala", "What in God's holy name are you blathering about?")

  val ProjectWithBogusScala =
    new ProjectMutableView(EmptyArtifactSource(), SimpleFileBasedArtifactSource(BogusScala))

  val HelloWorldScala = StringFileArtifact("Hello.scala",
    """
      |class Hello {
      |
      | println("Hello world")
      |}
    """.stripMargin)

  val Exceptions = StringFileArtifact("Exceptions.scala",
    """
      |class Catches {
      |
      | def doIt() {
      |   try {
      |      doSomething()
      |   }
      |   catch {
      |     case p: ThePlaneHasFlownIntoTheMountain =>
      |        println("thing")
      |   }
      |
      |   val s: Object = null
      |   s match {
      |     case p: ThePlaneHasFlownIntoTheMountain =>
      |       println("Foo bar")
      |   }
      | }
      |}
    """.stripMargin)

  val OldStyleScalaTest =
    """
      |// Don't worry about imports, we're not compiling the thing,
      |// it just needs to have a valid AST
      |
      |class TestLoaderTest extends FlatSpec with Matchers {
      |
      | it should s"find test scenarios under ${ac.testsRoot}" in {
      |    val scenarios = testLoader.loadTestScenarios(new SimpleFileBasedArtifactSource("",
      |      Seq(
      |        StringFileArtifact(s"${ac.testsRoot}/foo.rt", foobarScenario),
      |        StringFileArtifact(s"${ac.testsRoot}/deeper/baz.rt", bazScenario)))
      |    )
      |    scenarios.size should be (2)
      |    scenarios.map(sc => sc.name).toSet should equal (Set("Foobar", "Baz"))
      |  }
      |}
    """.stripMargin

  val ScalaTestSources = SimpleFileBasedArtifactSource(
    StringFileArtifact("src/test/scala/test/TestLoaderTest.scala", OldStyleScalaTest)
  )

  def helloWorldProject =
    new ProjectMutableView(EmptyArtifactSource(), SimpleFileBasedArtifactSource(HelloWorldScala))

  def exceptionsProject =
    new ProjectMutableView(EmptyArtifactSource(), SimpleFileBasedArtifactSource(Exceptions))

}