package com.atomist.tree.content.text.microgrammar.dsl

import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.spi.UsageSpecificTypeRegistry
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.TreeNode
import com.atomist.tree.content.text.microgrammar._
import com.atomist.tree.pathexpression.{PathExpressionEngine, PathExpressionParser}
import com.atomist.tree.utils.TreeNodeUtils
import org.scalatest.{FlatSpec, Matchers}

class MicrogrammarPathExpressionTest extends FlatSpec with Matchers {

  it should "Let me write the microgrammar this pretty way" in {
    val stringRegex =
      s"""${MatcherDefinitionParser.RegexpOpenToken}"[^"]*"${MatcherDefinitionParser.RegexpOpenToken}""" // this is not complete. valid Java string

    /* Story: I have this case class (really it was Regex but that's confusing here
     * so let's call it MyFunction) with 2 string args, name and regex.
     * I want to swap their order and make name into an Option[String].
     *
     * Find all usages of the function. Swap their args and make the name Some("whatever it was")
     */
    val topLevelGrammar =
      """MyFunction($nameArg, $reArg)"""

    val inners = Map(
      "nameArg" -> stringRegex,
      "reArg" -> stringRegex)

    val mg = MatcherMicrogrammarConstruction.matcherMicrogrammar("myFunctionCall", topLevelGrammar, inners)

    val pathExpression = "//File()/myFunctionCall()"

    val pretendScala =
      """package blahblah
        |import balahdlsifjd
        |
        |object Whatever {
        |   val f = MyFunction("iAmTheName","regex")
        |
        |   def somewhereElse = {
        |      e = MyFunction("AnotherName", "more regex")
        |   }
        |}
      """.stripMargin

    /* someday, expand this test to be in ts */
    val inTypescriptYouWould =
      s""" n -> n.update(`FunctionCall($${n.reArg()},Some($${n.nameArg()})`)"""

    val justFindTheMatches = mg.findMatches(pretendScala)
    justFindTheMatches.size should be(2)

    val result = ExercisePathExpression.exercisePathExpression(mg, pathExpression, pretendScala)

    result.size should be(2)
    val m = result.head
    withClue(println(TreeNodeUtils.toShortString(m))) {
      m.value should be("""MyFunction("iAmTheName","regex")""")
      m.childNodes.size should be(2)
      val nameArgNode = m.childNodes.head
      nameArgNode.value should be(""""iAmTheName"""")
      val reArgNode = m.childNodes(1)
      reArgNode.value should be(""""regex"""")
    }

  }
}

class OptionalFieldMicrogrammarTest extends FlatSpec with Matchers {

  import ExercisePathExpression._

  it should "Let give 0 matches when I access something that might exist but does not" in {

    val microgrammar =
      new MatcherMicrogrammar(
        Literal("a ") ~ Regex("[a-z]+", Some("blah")) ~ Optional(Literal("yo", Some("myWord"))) ~ Literal(".")
        , "bananagrammar")
    val pathExpression = "/File()/bananagrammar()/myWord()"

    val result = exercisePathExpression(microgrammar, pathExpression, input)

    assert(result.size === 0)
  }

  it should "Let give 0 matches when I access something deep that might exist but does not" in {

    val microgrammar =
      new MatcherMicrogrammar(
        Literal("a ") ~ Regex("[a-z]+", Some("blah")) ~ Optional(Literal("yo") ~ Wrap(Rep(Regex("[a-z]+", Some("carrot"))), "banana")) ~ Literal(".")
        , "bananagrammar")
    val pathExpression = "/File()/bananagrammar()/banana()/carrot()"

    val result = exercisePathExpression(microgrammar, pathExpression, input)

    assert(result.size === 0)
  }

  it should "Fail when I name a node that can't possibly exist" in pendingUntilFixed {

    val microgrammar =
      new MatcherMicrogrammar(
        Literal("a ") ~ Regex("[a-z]+", Some("blah"))
        , "bananagrammar")
    val pathExpression = "/File()/bananagrammar()/dadvan()"

    val message = exerciseFailingPathExpression(microgrammar, pathExpression, input)
    message should be("what should it be")
  }

}

object ExercisePathExpression extends FlatSpec with Matchers {
  // not a test but it needs fail()

  def exercisePathExpression(microgrammar: Microgrammar, pathExpressionString: String, input: String): List[TreeNode] = {

    val result = exercisePathExpressionInternal(microgrammar, pathExpressionString, input)
    result match {
      case Left(a) => fail(a)
      case Right(b) => b.map(_.asInstanceOf[TreeNode])
    }
  }

  private  def exercisePathExpressionInternal(microgrammar: Microgrammar, pathExpressionString: String, input: String) = {

    /* Construct a root node */
    val as = SimpleFileBasedArtifactSource(StringFileArtifact("banana.txt", input))
    val pmv = new ProjectMutableView(as /* cheating */ , as, DefaultAtomistConfig)

    /* Parse the path expression */

    /* Parse the path expression */
    val pathExpression = PathExpressionParser.parseString(pathExpressionString)

    /* Install the microgrammar */
    val typeRegistryWithMicrogrammar =
      new UsageSpecificTypeRegistry(DefaultTypeRegistry,
        Seq(new MicrogrammarTypeProvider(microgrammar)))

    new PathExpressionEngine().evaluate(pmv, pathExpression, typeRegistryWithMicrogrammar)
  }

  def exerciseFailingPathExpression(microgrammar: Microgrammar, pathExpressionString: String, input: String): String = {
    val result = exercisePathExpressionInternal(microgrammar, pathExpressionString, input)

    result match {
      case Left(a) => a
      case Right(b) => fail("This was supposed to fail")
    }
  }

  val input: String =
    """There was a banana. It crossed the street. A car ran over it.
      |No banana for you.
      |""".stripMargin

  it should "match an unnamed literal" in {

    val microgrammar = new MatcherMicrogrammar(Literal("banana"), "bananagrammar")
    val pathExpression = "/File()/bananagrammar()"

    val result = exercisePathExpression(microgrammar, pathExpression, input)

    assert(result.size === 2)
  }

  it should "match a named node" in {

    val microgrammar =
      new MatcherMicrogrammar(
        Literal("a ") ~ Regex("[a-z]+", Some("blah")) ~ Optional(Literal("yo", Some("myWord"))) ~ Literal(".")
        , "bananagrammar")
    val pathExpression = "/File()/bananagrammar()/blah"

    val result = exercisePathExpression(microgrammar, pathExpression, input)

    assert(result.size === 1)
  }
}