package com.atomist.rug.kind.scala

import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.kind.dynamic.MutableContainerMutableView
import com.atomist.rug.kind.grammar.AbstractTypeUnderFileTest
import com.atomist.source.{EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.content.text.{ConsoleMatchListener, TreeNodeOperations}
import com.atomist.tree.pathexpression.PathExpressionParser
import com.atomist.tree.utils.TreeNodeUtils
import com.atomist.tree.{MutableTreeNode, TreeNode}

class ScalaFileTypeTest extends AbstractTypeUnderFileTest {

  import PathExpressionParser._
  import ScalaFileTypeTest._

  override val typeBeingTested = new ScalaFileType

  it should "ignore ill-formed file without error" in {
    val scalas = typeBeingTested.findAllIn(projectWithBogusScala)
    // Should have silently ignored the bogus file
    assert(scalas.size === 1)
  }

  it should "parse hello world" in {
    val scalas = typeBeingTested.findAllIn(helloWorldProject)
    assert(scalas.size === 1)
  }

  it should "parse hello world and write out correctly" in {
    val parsed = typeBeingTested.fileToRawNode(HelloWorldScala, Some(ConsoleMatchListener)).get
    val parsedValue = parsed.value
    withClue(s"Unexpected content: [$parsedValue]") {
      parsedValue should equal(HelloWorldScala.content)
    }
  }

  it should "parse hello world into mutable view and write out unchanged" in {
    val scalas = typeBeingTested.findAllIn(helloWorldProject)
    assert(scalas.size === 1)
    scalas.head.head match {
      case mtn: MutableContainerMutableView =>
        val content = mtn.value
        content should equal(helloWorldProject.files.get(0).content)
    }
  }

  it should "find hello world using path expression" in {
    val expr = "//ScalaFile()"
    val rtn = expressionEngine.evaluate(helloWorldProject, expr, DefaultTypeRegistry)
    assert(rtn.right.get.size === 1)
  }

  it should "find specific exception catch" in {
    val scalas: Option[Seq[TreeNode]] = typeBeingTested.findAllIn(exceptionsProject)
    assert(scalas.size === 1)
    val scalaFileNode = scalas.get.head.asInstanceOf[MutableContainerMutableView]

    val expr = "//termTryWithCases/case//typeName[@value='ThePlaneHasFlownIntoTheMountain']"
    expressionEngine.evaluate(scalaFileNode, expr, DefaultTypeRegistry) match {
      case Right(nodes) if nodes.nonEmpty =>
        assert(nodes.size === 1)
        assert(nodes.head.value === "ThePlaneHasFlownIntoTheMountain")
      case wtf => fail(s"Expression didn't match [$wtf]. The tree was " + TreeNodeUtils.toShorterString(scalaFileNode))
    }

    assert(scalaFileNode.value === Exceptions.content)
  }

  it should "find and modify specific exception catch" in {
    val proj = exceptionsProject
    val scalas: Option[Seq[TreeNode]] = typeBeingTested.findAllIn(proj)
    assert(scalas.size === 1)
    val scalaFileNode = scalas.get.head.asInstanceOf[MutableContainerMutableView]

    val newException = "MicturationException"

    val expr = "//termTryWithCases/case//typeName[@value='ThePlaneHasFlownIntoTheMountain']"
    expressionEngine.evaluate(scalaFileNode, expr, DefaultTypeRegistry) match {
      case Right(nodes) if nodes.nonEmpty =>
        assert(nodes.size === 1)
        val mut = nodes.head.asInstanceOf[MutableContainerMutableView]
        mut.update(newException)
      case wtf =>
        fail(s"Expression didn't match [$wtf]. The tree was " + TreeNodeUtils.toShorterString(scalaFileNode))
    }

    val newContent = Exceptions.content.replaceFirst("ThePlaneHasFlownIntoTheMountain", newException)
    assert(scalaFileNode.value === newContent)

    assert(scalaFileNode.dirty === true)
    val updatedFile = proj.findFile(Exceptions.path)
    assert(updatedFile.content === newContent)
    //updatedFile.dirty should be(true)
  }

  it should "find and modify multiple points" in {
    val proj = exceptionsProject
    val scalas: Option[Seq[TreeNode]] = typeBeingTested.findAllIn(proj)
    assert(scalas.size === 1)
    val scalaFileNode = scalas.get.head.asInstanceOf[MutableContainerMutableView]

    val newException = "MicturationException"

    val expr = "//case//typeName[@value='ThePlaneHasFlownIntoTheMountain']"
    expressionEngine.evaluate(scalaFileNode, expr, DefaultTypeRegistry) match {
      case Right(nodes) if nodes.nonEmpty =>
        assert(nodes.size === 2)
        nodes.foreach {
          case mut: MutableContainerMutableView => mut.update(newException)
          case _ =>
        
        }
      case wtf =>
        fail(s"Expression didn't match [$wtf]. The tree was " + TreeNodeUtils.toShorterString(scalaFileNode))
    }

    val newContent = Exceptions.content.replaceAll("ThePlaneHasFlownIntoTheMountain", newException)
    assert(scalaFileNode.value === newContent)

    assert(scalaFileNode.dirty === true)
    val updatedFile = proj.findFile(Exceptions.path)
    assert(updatedFile.content === newContent)
    //updatedFile.dirty should be(true)
  }

  it should "find and modify specific exception catch body" in {
    val proj = exceptionsProject
    val scalas: Option[Seq[TreeNode]] = typeBeingTested.findAllIn(proj)
    assert(scalas.size === 1)
    val scalaFileNode = scalas.get.head.asInstanceOf[MutableContainerMutableView]

    val newException = "MicturationException"
    //println(TreeNodeUtils.toShorterString(scalaFileNode))

    val expr = "//termTryWithCases/case[//typeName[@value='ThePlaneHasFlownIntoTheMountain']]"
    expressionEngine.evaluate(scalaFileNode, expr, DefaultTypeRegistry) match {
      case Right(nodes) if nodes.nonEmpty =>
        assert(nodes.size === 1)
        val mut = nodes.head.asInstanceOf[MutableContainerMutableView]
        val terminals = TreeNodeOperations.terminals(mut)
        //println(terminals)
        terminals(1).asInstanceOf[MutableTreeNode].update(newException)
      case wtf =>
        fail(s"Expression didn't match [$wtf]. The tree was " + TreeNodeUtils.toShorterString(scalaFileNode))
    }

    val newContent = Exceptions.content.replaceFirst("ThePlaneHasFlownIntoTheMountain", newException)
    assert(scalaFileNode.value === newContent)

    assert(scalaFileNode.dirty === true)
    val updatedFile = proj.findFile(Exceptions.path)
    assert(updatedFile.content === newContent)
    //updatedFile.dirty should be(true)
  }

}

object ScalaFileTypeTest {

  val BogusScala = StringFileArtifact("Test.scala", "What in God's holy name are you blathering about?")

  def projectWithBogusScala =
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

  val OldStyleScalaTest = StringFileArtifact("src/test/scala/test/TestLoaderTest.scala",
    """
      |import org.scalatest.{FlatSpec, Matchers}
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
      |
      |    // Already OK
      |    assert("dog" === "cat")
      |  }
      |}
    """.stripMargin)

  val UsesDotEquals = StringFileArtifact("src/test/scala/main/UsesDotEquals.scala",
    """
      |class Foo {
      |
      | val no = "dog".equals("cat")
      |
      | // Won't handle infix yet
      |
      | val complexLeft = ("dog" + "gie").equals("cat")
      |
      | val complexRight = "dog".equals("pussy" + "cat")
      |
      | val becauseYouCanDoAnythingInScala = bar("a", "b").equals("dog" match {
      |   case s: String => true
      |   case _ => false
      | })
      |
      | def bar(a: String, b: String): Boolean = {
      |   a.equals(b)
      | }
      |}
    """.stripMargin)

  val Python3Source = StringFileArtifact("src/main/scala/com/atomist/PythonFileType.scala",
    """
      |package com.atomist
      |
      |class PythonFileType
      |  extends AntlrRawFileType("file_input",
      |    FromGrammarAstNodeCreationStrategy,
      |    grammar = "classpath:grammars/antlr/Python3.g4") {
      |
      |  import PythonFileType._
      |
      |  override def description = "Python file"
      |
      |  override def isOfType(f: FileArtifact): Boolean =
      |    f.name.endsWith(PythonExtension)
      |
      |}
      |
    """.stripMargin)

  val PythonTypeSources = SimpleFileBasedArtifactSource(
    Python3Source
  )

  val ScalaTestSources = SimpleFileBasedArtifactSource(
    OldStyleScalaTest
  )

  val UsesDotEqualsSources = SimpleFileBasedArtifactSource(
    UsesDotEquals,
    OldStyleScalaTest
  )

  def helloWorldProject =
    new ProjectMutableView(SimpleFileBasedArtifactSource(HelloWorldScala))


  def exceptionsProject =
    new ProjectMutableView(SimpleFileBasedArtifactSource(Exceptions))





}