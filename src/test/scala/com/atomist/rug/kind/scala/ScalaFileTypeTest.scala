package com.atomist.rug.kind.scala

import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.kind.dynamic.MutableContainerMutableView
import com.atomist.source.{EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.TreeNode
import com.atomist.tree.content.text.ConsoleMatchListener
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
    val scalas = scalaType.findAllIn(HelloWorldProject)
    scalas.size should be(1)
  }

  it should "parse hello world and write out correctly" in {
    val parsed = scalaType.contentToRawNode(HelloWorldScala.content, Some(ConsoleMatchListener)).get
    val parsedValue = parsed.value
    withClue(s"Unexpected content: [$parsedValue]") {
      parsedValue should equal(HelloWorldScala.content)
    }
  }

  it should "parse hello world into mutable view and write out unchanged" in {
    val scalas = scalaType.findAllIn(HelloWorldProject)
    scalas.size should be(1)
    scalas.head.head match {
      case mtn: MutableContainerMutableView =>
        val content = mtn.value
        content should equal(HelloWorldProject.files.get(0).content)
    }
  }

  it should "find hello world using path expression" in {
    val expr = "//ScalaFile()"
    val rtn = ee.evaluate(HelloWorldProject, expr, DefaultTypeRegistry)
    rtn.right.get.size should be(1)
  }

  it should "find specific exception catch" in {
    val scalas: Option[Seq[TreeNode]] = scalaType.findAllIn(ExceptionsProject)
    scalas.size should be(1)
    val scalaFileNode = scalas.get.head.asInstanceOf[MutableContainerMutableView]

    val expr = "//TermTryWithCases/Case//TypeName[@value='ThePlaneHasFlownIntoTheMountain']"
      ee.evaluate(scalaFileNode, expr, DefaultTypeRegistry) match {
        case Right(nodes) if nodes.nonEmpty =>
          nodes.size should be(1)
          nodes.head.value should be("ThePlaneHasFlownIntoTheMountain")
        case wtf => fail(s"Expression didn't match [$wtf]. The tree was " + TreeNodeUtils.toShorterString(scalaFileNode))
      }

    scalaFileNode.value should equal(Exceptions.content)
  }

  it should "find and modify specific exception catch" in {
    val proj = ExceptionsProject
    val scalas: Option[Seq[TreeNode]] = scalaType.findAllIn(proj)
    scalas.size should be(1)
    val scalaFileNode = scalas.get.head.asInstanceOf[MutableContainerMutableView]

    val newException = "MicturationException"

    val expr = "//TermTryWithCases/Case//TypeName[@value='ThePlaneHasFlownIntoTheMountain']"
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
    val proj = ExceptionsProject
    val scalas: Option[Seq[TreeNode]] = scalaType.findAllIn(proj)
    scalas.size should be(1)
    val scalaFileNode = scalas.get.head.asInstanceOf[MutableContainerMutableView]

    val newException = "MicturationException"

    val expr = "//Case//TypeName[@value='ThePlaneHasFlownIntoTheMountain']"
    ee.evaluate(scalaFileNode, expr, DefaultTypeRegistry) match {
      case Right(nodes) if nodes.nonEmpty =>
        nodes.size should be(1)
        val mut = nodes.head.asInstanceOf[MutableContainerMutableView]
        mut.update(newException)
      case wtf =>
        fail(s"Expression didn't match [$wtf]. The tree was " + TreeNodeUtils.toShorterString(scalaFileNode))
    }

    val newContent = Exceptions.content.replace("ThePlaneHasFlownIntoTheMountain", newException)
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

  val HelloWorldProject =
    new ProjectMutableView(EmptyArtifactSource(), SimpleFileBasedArtifactSource(HelloWorldScala))

  val ExceptionsProject =
    new ProjectMutableView(EmptyArtifactSource(), SimpleFileBasedArtifactSource(Exceptions))

}