package com.atomist.rug.kind.java

import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.kind.grammar.AbstractTypeUnderFileTest
import com.atomist.source.{EmptyArtifactSource, SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.content.text.{FormatInfo, OverwritableTextTreeNode}
import com.atomist.tree.pathexpression.PathExpressionParser
import com.atomist.tree.utils.TreeNodeUtils
import com.atomist.tree.{ParentAwareTreeNode, TreeNode}

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
    val parsedValue = parseAndPad(HelloWorldJava)
    val parsedAgain = parseAndPad(StringFileArtifact(HelloWorldJava.path, parsedValue))
    assert(parsedAgain === parsedValue)
  }

  it should "parse hello world into mutable view and write out unchanged" in {
    val javas = typeBeingTested.findAllIn(helloWorldProject)
    assert(javas.size === 1)
    javas.head.head match {
      case mtn =>
        val content = mtn.value
        val parsedAgain = parseAndPad(StringFileArtifact(HelloWorldJava.path, content))
        assert(mtn.value === parsedAgain)
    }
  }

  it should "find hello world using path expression" in {
    val expr = "//JavaFile()"
    val rtn = expressionEngine.evaluate(helloWorldProject, expr)
    assert(rtn.right.get.size === 1)
  }

  it should "find specific exception catch" in {
    val javas: Option[Seq[TreeNode]] = typeBeingTested.findAllIn(exceptionsProject)
    assert(javas.size === 1)
    val javaFileNode = javas.get.head

    val expr = "//catchClause//catchType[@value='ThePlaneHasFlownIntoTheMountain']"
    expressionEngine.evaluate(javaFileNode, expr) match {
      case Right(nodes) if nodes.nonEmpty =>
        assert(nodes.size === 1)
        assert(nodes.head.asInstanceOf[TreeNode].value === "ThePlaneHasFlownIntoTheMountain")
      case wtf => fail(s"Expression didn't match [$wtf]. The tree was " + TreeNodeUtils.toShorterString(javaFileNode))
    }
  }

  it should "find format info for specific exception catch" in nodeAndFormatInfoForPathExpression

  it should "find path to format info, execute and verify" in {
    val (proj, tn, fi) = nodeAndFormatInfoForPathExpression
    // Get path and find it again
    val path = proj.pathTo(Exceptions.path, "JavaFile", fi.start.lineNumberFrom1, fi.start.columnNumberFrom1)
    assert(path.contains("JavaFile"))
    //println(s"Running path " + path)
    expressionEngine.evaluate(proj, path) match {
      case Right(nodes) if nodes.size == 1 =>
        val found = nodes.head.asInstanceOf[TreeNode]
        assert(found.value === tn.value)
        assert(found.nodeName === tn.nodeName)
      case wtf => fail(s"Unexpected $wtf")
    }
  }

  it should "find path the node above, when the character requested lies in padding" in {
    // this fetches "ThePlaneHasFlown..." in "catch (ThePlaneHasFlown..."
    val (proj, tn, fi) = nodeAndFormatInfoForPathExpression
    // Get path of the "catch"
    val path = proj.pathTo(Exceptions.path, "JavaFile", fi.start.lineNumberFrom1, fi.start.columnNumberFrom1 - 5)
    assert(path.contains("JavaFile"))
    //println(s"Running path " + path)
    expressionEngine.evaluate(proj, path) match {
      case Right(nodes) if nodes.size == 1 =>
        val found = nodes.head.asInstanceOf[TreeNode]
        val catchesFromCatchType = significantParent(tn.parent.asInstanceOf[ParentAwareTreeNode])
        assert(found.nodeName === catchesFromCatchType.nodeName)
        assert(found.value === catchesFromCatchType.value)
      case wtf => fail(s"Unexpected $wtf")
    }
  }

  // if the parent has the same value, you'll get the parent back from nodeAt, so keep looking
  def significantParent(tn: ParentAwareTreeNode) = {
    def skipInsignificant(tn: TreeNode): TreeNode = {
      tn match {
        case lessInteresting: ParentAwareTreeNode if lessInteresting.value == lessInteresting.parent.value =>
          skipInsignificant(lessInteresting.parent)
        case other
        => other
      }
    }

    skipInsignificant(tn.parent)
  }

  private def nodeAndFormatInfoForPathExpression: (ProjectMutableView, OverwritableTextTreeNode, FormatInfo) = {
    val expectedValue = "ThePlaneHasFlownIntoTheMountain"
    val proj = exceptionsProject
    val expr = s"//JavaFile()//catchClause//catchType[@value='$expectedValue']"
    expressionEngine.evaluate(proj, expr) match {
      case Right(nodes) if nodes.nonEmpty =>
        assert(nodes.size === 1)
        val tn = nodes.head.asInstanceOf[OverwritableTextTreeNode]
        assert(tn.value === expectedValue)
        val fi = tn.formatInfo
        assert(fi.start.lineNumberFrom1 > 1)
        assert(fi.start.columnNumberFrom1 > 1)
        //println(fi)
        val foundInFile = Exceptions.content.substring(fi.start.offset, fi.end.offset)
        assert(foundInFile === expectedValue)
        (proj, tn, fi)
      case wtf => fail(s"Unexpected: $wtf")
    }
  }

  it should "find and modify specific exception catch" in {
    val proj = exceptionsProject
    val javas: Option[Seq[TreeNode]] = typeBeingTested.findAllIn(proj)
    assert(javas.size === 1)
    val javaFileNode = javas.get.head

    val newException = "MicturationException"

    val expr = "//catchClause//catchType[@value='ThePlaneHasFlownIntoTheMountain']"
    expressionEngine.evaluate(javaFileNode, expr) match {
      case Right(nodes) if nodes.nonEmpty =>
        assert(nodes.size === 1)
        val mut = nodes.head.asInstanceOf[OverwritableTextTreeNode]
        mut.update(newException)
      case wtf =>
        fail(s"Expression didn't match [$wtf]. The tree was " + TreeNodeUtils.toShorterString(javaFileNode))
    }

    val newContent = Exceptions.content.replaceFirst("ThePlaneHasFlownIntoTheMountain", newException)
    withClue(s"the node contains <${javaFileNode.value}>") {
      assert(proj.files.get(0).content === newContent)
    }

    val updatedFile = proj.findFile(Exceptions.path)
    assert(updatedFile.content === newContent)
    // updatedFile.dirty shouldBe true
  }
}

object JavaFileTypeTest {

  val BogusJava = StringFileArtifact("Test.scala", "What in God's holy name are you blathering about?")

  def projectWithBogusJava =
    new ProjectMutableView(EmptyArtifactSource(), SimpleFileBasedArtifactSource(BogusJava))
  
  /** So simple it doesn't even have a newline */
  val HelloWorldJava = StringFileArtifact("Hello.java", "public class Hello { }")

  val Exceptions = StringFileArtifact("Exceptions.java",
    """
      | public class Catches {
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

  val ExceptionsSources = SimpleFileBasedArtifactSource(Exceptions)

  def helloWorldProject =
    new ProjectMutableView(EmptyArtifactSource(), SimpleFileBasedArtifactSource(HelloWorldJava))

  def exceptionsProject =
    new ProjectMutableView(EmptyArtifactSource(), ExceptionsSources)
}
