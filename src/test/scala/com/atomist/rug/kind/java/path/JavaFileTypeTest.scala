package com.atomist.rug.kind.java.path

import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.kind.dynamic.MutableContainerMutableView
import com.atomist.rug.kind.grammar.AbstractTypeUnderFileTest
import com.atomist.source.{EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.TreeNode
import com.atomist.tree.content.text.ConsoleMatchListener
import com.atomist.tree.pathexpression.PathExpressionParser
import com.atomist.tree.utils.TreeNodeUtils

class JavaFileTypeTest extends AbstractTypeUnderFileTest {

  import JavaFileTypeTest._
  import PathExpressionParser._

  override val typeBeingTested = new JavaFileType


  it should "ignore ill-formed file without error" in {
    val javas = typeBeingTested.findAllIn(projectWithBogusJava)
    // Should have silently ignored the bogus file
    assert(javas.size === 1)
  }

  it should "parse hello world" in {
    val javas = typeBeingTested.findAllIn(helloWorldProject)
    assert(javas.size === 1)
  }

  it should "parse hello world and write out correctly" in {
    val parsed = typeBeingTested.fileToRawNode(HelloWorldJava, Some(ConsoleMatchListener)).get
    val parsedValue = parsed.value
    val parsedAgain = typeBeingTested.fileToRawNode(StringFileArtifact(HelloWorldJava.path, parsedValue)).get
    assert(parsedAgain.value === parsed.value)
  }

  it should "allow field to be added conveniently" is pending

  it should "allow method to be added conveniently" is pending

  it should "parse hello world into mutable view and write out unchanged" in {
    val javas = typeBeingTested.findAllIn(helloWorldProject)
    assert(javas.size === 1)
    javas.head.head match {
      case mtn: MutableContainerMutableView =>
        val content = mtn.value
        val parsedAgain = typeBeingTested.fileToRawNode(StringFileArtifact(HelloWorldJava.path, content)).get
        assert(mtn.value === parsedAgain.value)
    }
  }

  it should "find hello world using path expression" in {
    val expr = "//JavaFile()"
    val rtn = expressionEngine.evaluate(helloWorldProject, expr, DefaultTypeRegistry)
    assert(rtn.right.get.size === 1)
  }

  it should "find specific exception catch" in {
    val javas: Option[Seq[TreeNode]] = typeBeingTested.findAllIn(exceptionsProject)
    assert(javas.size === 1)
    val javaFileNode = javas.get.head.asInstanceOf[MutableContainerMutableView]

    val expr = "//catchClause//catchType[@value='ThePlaneHasFlownIntoTheMountain']"
    expressionEngine.evaluate(javaFileNode, expr, DefaultTypeRegistry) match {
      case Right(nodes) if nodes.nonEmpty =>
        assert(nodes.size === 1)
        assert(nodes.head.value === "ThePlaneHasFlownIntoTheMountain")
      case wtf => fail(s"Expression didn't match [$wtf]. The tree was " + TreeNodeUtils.toShorterString(javaFileNode))
    }
  }

  it should "find and modify specific exception catch" in {
    val proj = exceptionsProject
    val javas: Option[Seq[TreeNode]] = typeBeingTested.findAllIn(proj)
    assert(javas.size === 1)
    val javaFileNode = javas.get.head.asInstanceOf[MutableContainerMutableView]

    val newException = "MicturationException"

    val expr = "//catchClause//catchType[@value='ThePlaneHasFlownIntoTheMountain']"
    expressionEngine.evaluate(javaFileNode, expr, DefaultTypeRegistry) match {
      case Right(nodes) if nodes.nonEmpty =>
        assert(nodes.size === 1)
        val mut = nodes.head.asInstanceOf[MutableContainerMutableView]
        println(s"Mut content was [${mut.value}]")
        mut.update(newException)
      case wtf =>
        fail(s"Expression didn't match [$wtf]. The tree was " + TreeNodeUtils.toShorterString(javaFileNode))
    }

    val newContent = Exceptions.content.replaceFirst("ThePlaneHasFlownIntoTheMountain", newException)
    assert(javaFileNode.value === newContent)

    assert(javaFileNode.dirty === true)
    val updatedFile = proj.findFile(Exceptions.path)
    assert(updatedFile.content === newContent)
    //updatedFile.dirty should be(true)
  }

}

object JavaFileTypeTest {

  val BogusJava = StringFileArtifact("Test.scala", "What in God's holy name are you blathering about?")

  def projectWithBogusJava =
    new ProjectMutableView(EmptyArtifactSource(), SimpleFileBasedArtifactSource(BogusJava))

  /** So simple it doesn't even have a newline */

  /** So simple it doesn't even have a newline */
  val HelloWorldJava = StringFileArtifact("Hello.java", "public class Hello { }")

  val Exceptions = StringFileArtifact("Exceptions.java",
    """
      |public class Catches {
      |
      | public void doIt() {
      |   try {
      |      doSomething();
      |   }
      |   catch (ThePlaneHasFlownIntoTheMountain p) {
      |        System.out.println("thing");
      |   }
      | }
      |}
    """.stripMargin)

  def helloWorldProject =
    new ProjectMutableView(EmptyArtifactSource(), SimpleFileBasedArtifactSource(HelloWorldJava))


  def exceptionsProject =
    new ProjectMutableView(EmptyArtifactSource(), SimpleFileBasedArtifactSource(Exceptions))





}

