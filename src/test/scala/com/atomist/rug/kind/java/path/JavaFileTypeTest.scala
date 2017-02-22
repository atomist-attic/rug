package com.atomist.rug.kind.java.path

import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.rug.kind.DefaultTypeRegistry
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
    val rtn = expressionEngine.evaluate(helloWorldProject, expr, DefaultTypeRegistry)
    assert(rtn.right.get.size === 1)
  }

  it should "find specific exception catch" in {
    val javas: Option[Seq[TreeNode]] = typeBeingTested.findAllIn(exceptionsProject)
    assert(javas.size === 1)
    val javaFileNode = javas.get.head

    val expr = "//catchClause//catchType[@value='ThePlaneHasFlownIntoTheMountain']"
    expressionEngine.evaluate(javaFileNode, expr, DefaultTypeRegistry) match {
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
    expressionEngine.evaluate(proj, path, DefaultTypeRegistry) match {
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
    expressionEngine.evaluate(proj, path, DefaultTypeRegistry) match {
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
    expressionEngine.evaluate(proj, expr, DefaultTypeRegistry) match {
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
    expressionEngine.evaluate(javaFileNode, expr, DefaultTypeRegistry) match {
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
    //updatedFile.dirty should be(true)
  }


  it should "map classDeclaration to Class" in {
    val expr1 = "/File()//JavaFile()//Class()"
    val rtn1 = expressionEngine.evaluate(manyClassesProject, PathExpressionParser.parseString(expr1), DefaultTypeRegistry)
    assert(rtn1.right.get.size === 2)

    val expr2 = "/File()//JavaFile()//classDeclaration()"
    val rtn2 = expressionEngine.evaluate(manyClassesProject, PathExpressionParser.parseString(expr2), DefaultTypeRegistry)
    assert(rtn2.right.get.size === 2)

    assert(rtn1.right.get.size === rtn2.right.get.size)
  }

  it should "map methodDeclaration to Method" in {
    val expr1 = "/File()/JavaFile()//Method()"
    val rtn1 = expressionEngine.evaluate(manyMethodsProject, PathExpressionParser.parseString(expr1), DefaultTypeRegistry)
    assert(rtn1.right.get.size === 3)

    val expr2 = "/File()/JavaFile()//methodDeclaration()"
    val rtn2 = expressionEngine.evaluate(manyMethodsProject, PathExpressionParser.parseString(expr2), DefaultTypeRegistry)
    assert(rtn2.right.get.size === 3)

    assert(rtn1.right.get.size === rtn2.right.get.size)
  }

  it should "map formalParameter to Args" in {
    val expr1 = "/File()/JavaFile()//Method()[/methodHeader/methodDeclarator/Identifier[@value='twoArgs']]//Args()"
    val rtn1 = expressionEngine.evaluate(manyMethodsProject, PathExpressionParser.parseString(expr1), DefaultTypeRegistry)
    assert(rtn1.right.get.size === 2)

    val expr2 = "/File()/JavaFile()//methodDeclaration()[/methodHeader/methodDeclarator/Identifier[@value='twoArgs']]//Args()"
    val rtn2 = expressionEngine.evaluate(manyMethodsProject, PathExpressionParser.parseString(expr2), DefaultTypeRegistry)
    assert(rtn2.right.get.size === 2)

    val expr3 = "/File()/JavaFile()//methodDeclaration()[/methodHeader/methodDeclarator/Identifier[@value='twoArgs']]//formalParameter()"
    val rtn3 = expressionEngine.evaluate(manyMethodsProject, PathExpressionParser.parseString(expr3), DefaultTypeRegistry)
    assert(rtn3.right.get.size === 2)
  }


  it should "map variable formalParameter to Args" in {
    val expr1 = "/File()/JavaFile()//Method()[/methodHeader/methodDeclarator/Identifier[@value='variableArgs']]//VarArgs()"
    val rtn1 = expressionEngine.evaluate(manyMethodsProject, PathExpressionParser.parseString(expr1), DefaultTypeRegistry)
    assert(rtn1.right.get.size === 1)

    val expr2 = "/File()/JavaFile()//methodDeclaration()[/methodHeader/methodDeclarator/Identifier[@value='variableArgs']]//VarArgs()"
    val rtn2 = expressionEngine.evaluate(manyMethodsProject, PathExpressionParser.parseString(expr2), DefaultTypeRegistry)
    assert(rtn2.right.get.size === 1)

    val expr3 = "/File()/JavaFile()//methodDeclaration()[/methodHeader/methodDeclarator/Identifier[@value='variableArgs']]//lastFormalParameter()"
    val rtn3 = expressionEngine.evaluate(manyMethodsProject, PathExpressionParser.parseString(expr3), DefaultTypeRegistry)
    assert(rtn3.right.get.size === 1)

  }

  it should "map lambdaExpression to Lambda" in {
    val expr1 = "/File()/JavaFile()//Lambda()"
    val rtn1 = expressionEngine.evaluate(singleLambdaExpressionProject, PathExpressionParser.parseString(expr1), DefaultTypeRegistry)
    assert(rtn1.right.get.size === 1)

    val expr2 = "/File()/JavaFile()//lambdaExpression()"
    val rtn2 = expressionEngine.evaluate(singleLambdaExpressionProject, PathExpressionParser.parseString(expr2), DefaultTypeRegistry)
    assert(rtn2.right.get.size === 1)

    assert(rtn1.right.get.size === rtn2.right.get.size)
  }

  it should "map classDeclaration and methodDeclaration to Class and Method" in {
    val expr1 = "/File()/JavaFile()//Class()//Method()"
    val rtn1 = expressionEngine.evaluate(singleMethodProject, PathExpressionParser.parseString(expr1), DefaultTypeRegistry)
    assert(rtn1.right.get.size === 1)

    val expr2 = "/File()/JavaFile()//classDeclaration()//methodDeclaration()"
    val rtn2 = expressionEngine.evaluate(singleMethodProject, PathExpressionParser.parseString(expr2), DefaultTypeRegistry)
    assert(rtn2.right.get.size === 1)

    assert(rtn1.right.get.size === rtn2.right.get.size)
  }

  it should "map importDeclaration to Import" in {
    val expr1 = "/File()/JavaFile()//Import()"
    val rtn1 = expressionEngine.evaluate(manyImportsProject, PathExpressionParser.parseString(expr1), DefaultTypeRegistry)
    assert(rtn1.right.get.size === 2)

    val expr2 = "/File()/JavaFile()//importDeclaration()"
    val rtn2 = expressionEngine.evaluate(manyImportsProject, PathExpressionParser.parseString(expr2), DefaultTypeRegistry)
    assert(rtn2.right.get.size === 2)

    assert(rtn1.right.get.size === rtn2.right.get.size)
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

  val EmptyClass1 = StringFileArtifact("EmptyClass1.java",
    """
      |public class EmptyClass1 {}
    """.stripMargin)

  val EmptyClass2 = StringFileArtifact("EmptyClass2.java",
    """
      |public class EmptyClass2 {}
    """.stripMargin)

  val SingleMethodInClass = StringFileArtifact("SingleMethodInClass.java",
    """
      |public class SingleMethodInClass {
      | public void echo(String message) {}
      |}
    """.stripMargin)

  val ManyMethodsInClass = StringFileArtifact("ManyMethodsInClass.java",
    """
      |public class ManyMethodsInClass {
      | public void echo(String message) {}
      | public void twoArgs(String a, String b) {}
      | private void variableArgs(String ... names) {}
      |}
    """.stripMargin)

  val ManyImportStatements = StringFileArtifact("ManyImportStatements.java",
    """
      |import java.io;
      |import java.math.*;
      |
      |public class DoNothingClass {}
    """.stripMargin)


  val SingleLambdaExpression = StringFileArtifact("SingleLambdaExpression.java",
    """
      |public class SingleLambdaExpression {
      | public static void main(String[] args) {
      |   Runnable r = () -> System.out.println("Hello world!");
      |   r.run();
      | }
      |}
    """.stripMargin)

  def helloWorldProject =
    new ProjectMutableView(EmptyArtifactSource(), SimpleFileBasedArtifactSource(HelloWorldJava))

  def exceptionsProject =
    new ProjectMutableView(EmptyArtifactSource(), SimpleFileBasedArtifactSource(Exceptions))

  def manyClassesProject =
    new ProjectMutableView(EmptyArtifactSource(),
      new SimpleFileBasedArtifactSource("name",
        Seq(EmptyClass1, EmptyClass2)
      )
    )

  def manyMethodsProject =
    new ProjectMutableView(EmptyArtifactSource(), SimpleFileBasedArtifactSource(ManyMethodsInClass))

  def singleMethodProject =
    new ProjectMutableView(EmptyArtifactSource(), SimpleFileBasedArtifactSource(SingleMethodInClass))

  def manyImportsProject =
    new ProjectMutableView(EmptyArtifactSource(), SimpleFileBasedArtifactSource(ManyImportStatements))

  def singleLambdaExpressionProject =
    new ProjectMutableView(EmptyArtifactSource(), SimpleFileBasedArtifactSource(SingleLambdaExpression))
}
