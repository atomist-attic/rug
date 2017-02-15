package com.atomist.tree.content.text.microgrammar

import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.source.{SimpleFileBasedArtifactSource, StringFileArtifact}
import com.atomist.tree.content.text.grammar.{AbstractMatchListener, MatchListener, PositionalString}
import com.atomist.tree.content.text.microgrammar.matchers.Break
import com.atomist.tree.content.text._
import com.atomist.tree.utils.TreeNodeUtils
import com.atomist.tree._
import org.scalatest.{FlatSpec, Matchers}

import scala.util.{Left, Right}

object MatcherMicrogrammarTest extends FlatSpec {

  def strictMatchAndFakeAFile(g: MatcherMicrogrammar, input: String): UpdatableTreeNode = {
    g.strictMatch(input) match {
      case Left(a) => fail(s"Failed to match. ${DismatchReport.detailedReport(a, input)}")
      case Right(b) =>
        readyMatchesFromString(Seq(b), input).head
    }
  }

  def readyMatchesFromString(matches: Seq[PositionedTreeNode], input: String) = {
    val as = SimpleFileBasedArtifactSource(StringFileArtifact("whatever", input))
    val pmv = new ProjectMutableView(as, as)
    val fmv = pmv.files.get(0)
    TextTreeNodeLifecycle.makeReady("fake", matches, fmv)
  }

}

class MatcherMicrogrammarTest extends FlatSpec with Matchers {

  import Literal._
  import MatcherMicrogrammarTest._

  it should "parse and update complete match" in {
    val g: MatcherMicrogrammar = thingGrammar
    val input = "This is a test"
    val m = strictMatchAndFakeAFile(g, input)
    m.count should be >= 1
    m.childrenNamed("thing").head match {
      case sm: UpdatableTreeNode =>
        assert(sm.nodeName === "thing")
        assert(sm.value === input)
        val newContent = "This is the new content"
        sm.update(newContent)
        assert(m.value === newContent)
    }
  }

  it should "parse 1 match of 2 parts in whole string" in {
    val matches = strictMatchAndFakeAFile(aWasaB, "Henry was aged 19")
    assert(matches.count === 2)
    matches.childrenNamed("name").head match {
      case sm: TreeNode =>
        assert(sm.value === "Henry")
    }
    matches.childrenNamed("age").head match {
      case sm: TreeNode =>
        assert(sm.value === "19")
    }
  }

  /**
    * Match all input, which must exactly match input.
    */
  it should "parse 1 match of 2 parts in whole string and replace both keys" in {
    val g = aWasaB
    val input = "Henry was aged 19"
    val matches = strictMatchAndFakeAFile(g, input)
    assert(matches.value === input)
    matches.count should be >= 2
    matches.childrenNamed("name").head match {
      case sm: UpdatableTreeNode =>
        assert(sm.value === "Henry")
        sm.update("Dave")
    }
    matches.childrenNamed("age").head match {
      case sm: UpdatableTreeNode =>
        assert(sm.value === "19")
        sm.update("40")
    }
    val currentContent = matches.value
    currentContent should be("Dave was aged 40")
  }

  it should "parse 1 match of 2 parts discarding prefix" in {
    val g = aWasaB
    val input = "rubbish goes here. Henry was aged 12"
    val m = MatcherMicrogrammarTest.readyMatchesFromString(g.findMatches(input), input)
    assert(m.size === 1)
    m.head.count should be >= 2
    m.head.childrenNamed("name").head match {
      case sm: TreeNode =>
        assert(sm.value === "Henry")
      case _ => ???
    }
    m.head.childrenNamed("age").head match {
      case sm: TreeNode =>
        assert(sm.value === "12")
      case _ => ???
    }
  }

  it should "parse 1 match of 2 parts discarding suffix" in {
    val g = aWasaB
    val s = "Henry was aged 24. Blah blah"
    val m = MatcherMicrogrammarTest.readyMatchesFromString(g.findMatches(s), s)
    assert(m.size === 1)
    m.head.count should be >= 2
    m.head.childrenNamed("name").head match {
      case sm: TreeNode =>
        assert(sm.value === "Henry")
      case _ => ???
    }
    m.head.childrenNamed("age").head match {
      case sm: TreeNode =>
        assert(sm.value === "24")
      case _ => ???
    }
  }

  it should "parse 2 matches interspersed with stuff" in
    twoMatchesSplit("It is a shame. Tony was aged 24. Alice was aged 16. And they are both gone")

  it should "parse 2 matches interspersed with stuff in longer text" in
    twoMatchesSplit(
      """
        |It is a shame. Tony was aged 24.
        |I could go on and on about irrelevant stuff--and probably will.
        |And the fact is, Alice was aged 16. And she and Tony are both gone
      """.stripMargin
    )

  private def twoMatchesSplit(input: String) {
    val g = aWasaB
    val positioned = g.findMatches(input)
    assert(positioned.size === 2)
    positioned.head.count should be >= 2
    val ready = MatcherMicrogrammarTest.readyMatchesFromString(positioned, input)
    val t = ready(1)
    t.childrenNamed("name").head match {
      case sm: TreeNode =>
        assert(sm.value === "Alice")
      case _ => ???
    }
    t.childrenNamed("age").head match {
      case sm: TreeNode =>
        assert(sm.value === "16")
      case _ => ???
    }
  }

  it should "parse 1 scala method without parameters" in {
    val g = matchScalaMethodHeaderRepsep
    val input =
      """
        |def foo(): Int = {
        | 0
        |}
      """.stripMargin
    val m = MatcherMicrogrammarTest.readyMatchesFromString(g.findMatches(input), input)
    assert(m.size === 1)
    m.head.childrenNamed("name").head match {
      case sm: TreeNode =>
        assert(sm.value === "foo")
      case _ => ???
    }
    m.head.childrenNamed("type").head match {
      case sm: TreeNode =>
        assert(sm.value === "Int")
      case _ => ???
    }
  }

  it should "parse 1 scala method with repsep parameters" in
    matchScalaMethodHeaderUsing(matchScalaMethodHeaderRepsep)

  //  IDENTIFIER : [a-zA-Z0-9]+;
  //  LPAREN : '(';
  //  RPAREN : ')';
  //  param_def : name=IDENTIFIER ':' type=IDENTIFIER;
  //  params : param_def (',' param_def)*;
  //  method : 'def' name=IDENTIFIER LPAREN params? RPAREN ':' type=IDENTIFIER;
  protected def matchScalaMethodHeaderRepsep: Microgrammar = {
    val identifier = Regex("[a-zA-Z0-9]+", Some("identifier"))
    val paramDef = Wrap(
      identifier.copy(givenName = Some("name")) ~? ":" ~? identifier.copy(givenName = Some("type")),
      "param_def")
    val params = Repsep(paramDef, ",", None)
    val method = "def" ~~ identifier.copy(givenName = Some("name")) ~? "(" ~?
      Wrap(params, "params") ~? ")" ~? ":" ~? identifier.copy(givenName = Some("type"))
    new MatcherMicrogrammar(method)

  }

  private def matchScalaMethodHeaderUsing(mg: Microgrammar, ml: Option[MatchListener] = None) {
    val input =
      """
        |def bar(barParamName: String): Unit = {
        |}
      """.stripMargin
    val m = MatcherMicrogrammarTest.readyMatchesFromString(mg.findMatches(input, ml), input)
    if (ml.isDefined) assert(ml.get.matches === 1)
    assert(m.size === 1)
    //println(TreeNodeUtils.toShortString(m.head))
    m.head.childrenNamed("name").head match {
      case sm: TreeNode =>
        assert(sm.value === "bar")
      case _ => ???
    }
    m.head.childrenNamed("type").head match {
      case sm: TreeNode =>
        assert(sm.value === "Unit")
      case _ => ???
    }
    val params = m.head.childrenNamed("params")
    val paramDef1 = params.head.childrenNamed("param_def")
    assert(paramDef1.size === 1)
    paramDef1.head match {
      case ov: ContainerTreeNode =>
        ov.childrenNamed("name").toList match {
          case (fv: TreeNode) :: Nil =>
            assert(fv.nodeName === "name")
            assert(fv.value === "barParamName")
          case _ => ???
        }
        ov.childrenNamed("type").toList match {
          case (fv: TreeNode) :: Nil =>
            assert(fv.nodeName === "type")
            assert(fv.value === "String")
          case _ => ???
        }
      case _ => ???
    }
  }

  it should "match method with multiple parameters" in {
    val input =
      """
        |class Something {
        |
        | def single(a: Int,b:Int):Int = a + b
        |}
      """.stripMargin
    val m = MatcherMicrogrammarTest.readyMatchesFromString(matchScalaMethodHeaderRepsep.findMatches(input), input)
    assert(m.size === 1)
    val last = m.head
    val params = last.childrenNamed("params").head.asInstanceOf[ContainerTreeNode].childrenNamed("param_def")
    params.size should be >= 2
    params foreach {
      case soo: TreeNode =>
        assert(soo.childrenNamed("type").head.value === "Int")
        val value = soo.childrenNamed("name").head.value
        Set("a", "b").contains(value) should be(true)
      case _ => ???
    }
  }

  it should "parse several scala method headers in other content without whitespace" in
    parseSeveralScalaMethodHeadersInOtherContent("", "")

  it should "parse several scala method headers in other content with spaces" in
    parseSeveralScalaMethodHeadersInOtherContent("   ", "  \t")

  it should "parse several scala method headers in other content with other whitespace" in
    parseSeveralScalaMethodHeadersInOtherContent(" ", " ")

  private def parseSeveralScalaMethodHeadersInOtherContent(pad1: String, pad2: String) {
    val input =
      s"""
         |class Something {
         |
         | def bar(barParamName: String): Unit = {
         | }
         |
         | def baz(): String = null
         |
         | def other(a: Int,${pad1}b$pad1:${pad2}Int): Int = a + b
         |}
      """.stripMargin
    val m = MatcherMicrogrammarTest.readyMatchesFromString(matchScalaMethodHeaderRepsep.findMatches(input), input)
    assert(m.size === 3)
    m.head.childrenNamed("name").head match {
      case sm: TreeNode =>
        assert(sm.value === "bar")
      case _ => ???
    }
    m.head.childrenNamed("type").head match {
      case sm: TreeNode =>
        assert(sm.value === "Unit")
      case _ => ???
    }
    val last = m(2)
    val params = last.childrenNamed("params").head.childrenNamed("param_def")
    params.size should be >= 2
    params foreach {
      case soo: TreeNode =>
        assert(soo.childrenNamed("type").head.value === "Int")
        val value = soo.childrenNamed("name").head.value
        Set("a", "b").contains(value) should be(true)
      case _ => ???
    }
  }

  it should "parse several annotated Java fields" in {
    val g = matchAnnotatedJavaFields
    val input =
      """
        |public class Animals {
        |
        | @Dangerous
        | private Hippopotamus hippo;
        |
        | @Sweet
        | private Fox fennecFox;
        |
        | public Animals() {
        | }
        |}
      """.stripMargin
    val m = MatcherMicrogrammarTest.readyMatchesFromString(g.findMatches(input), input)
    assert(m.size === 2)
    m.head.childrenNamed("name").head match {
      case sm: TreeNode =>
        assert(sm.value === "hippo")
      case _ => ???
    }
    m.head.childrenNamed("type").head match {
      case sm: TreeNode =>
        assert(sm.value === "Hippopotamus")
      case _ => ???
    }
  }

  // Test class generating behavior and ambiguity
  it should "parse annotated Java fields with other productions with same name" in {
    val input =
      """
        |public class Animals {
        |
        | @Dangerous
        | private Hippopotamus hippo;
        |
        | @Sweet
        | private Fox fennecFox;
        |
        | public Animals() {
        | }
        |}
      """.stripMargin
    val m = MatcherMicrogrammarTest.readyMatchesFromString(matchAnnotatedJavaFields.findMatches(input), input)
    assert(m.size === 2)
    m.head.childrenNamed("name").head match {
      case sm: TreeNode =>
        assert(sm.value === "hippo")
    }
    m.head.childrenNamed("type").head match {
      case sm: TreeNode =>
        assert(sm.value === "Hippopotamus")
    }
    val m2 = matchPrivateJavaFields.findMatches(input)
    assert(m2.size === 2)
  }

  class SavingMatchListener extends AbstractMatchListener("test") {
    var hits: List[TreeNode] = Nil
    var skipped: List[PositionalString] = Nil

    override protected def onMatchInternal(m: PositionedTreeNode): Unit = {
      hits = hits :+ m
    }

    override def onSkip(junk: PositionalString): Unit = {
      skipped = skipped :+ junk
    }
  }

  it should "parse several annotated Java fields using match listener" in {
    val g = matchAnnotatedJavaFields
    val input =
      """
        |public class Animals {
        |
        | @Dangerous
        | private Hippopotamus hippo;
        |
        | @Sweet
        | private Fox fennecFox;
        |
        | public Animals() {
        | }
        |}
      """.stripMargin
    val ml = new SavingMatchListener
    val m = MatcherMicrogrammarTest.readyMatchesFromString(g.findMatches(input, Some(ml)), input)
    assert(m.size === 2)
    m.head.childrenNamed("name").head match {
      case sm: TreeNode =>
        assert(sm.value === "hippo")
    }
    m.head.childrenNamed("type").head match {
      case sm: TreeNode =>
        assert(sm.value === "Hippopotamus")
    }
    assert(ml.matches === 2)
    //ml.hits.foreach(_.asInstanceOf[MutableObjectFieldValueImpl].startPosition.asInstanceOf[LineInputPosition].lineFrom1 should be > (1))
    //ml.skipped.size should be > (0)
  }

  it should "edit yml" in {
    val input =
      """
        |language: java
        |script: bash ./travis-build.sh
        |jdk:
        |- oraclejdk8
        |env:
        |  global:
        |  - CI_DEPLOY_USERNAME=travis-mvn-deploy
        |  - secure: nnWB6oO1NDgLHLCpiDiuWHnfh3t66KkE0K6Z1rbYv/uGMAMP+8R/YkrRJFpRRB2YoOCTJ+nxefjeEJTqlXlz9+tNLO90ctxyabH6QMnCT+KC/S237GxjczXXP1eFI5r8PKuY1Hdf7G1YIFhH9RKS8lFJjDbV4IX70hFJynj6lQhu/eLhjh6CRFpWPCFHrZd1k3OVVQ4WHfumKpBxHp/0hAe+BFLO3HlIuZYbyChLWzvYpc0yPRTOd82i2jZ+JUotlQcZ5ttyvCj4QjCNjPvg6zjcpPo+qK7Oh9R5wvYHmDkOmjSPO83COz0uvFO1XBKuINLJM6Iwc7Aw8wptmDxTlxcKbf7wPB8r7KWq6uT2WdrG/euUtI76k137SZOx4BmgJARuUr6FXyOjzoba10531O3T9bzIgXbxcR50NU5UWpMLqjgYfNTezs9PtQLXZMBxMYeCsVt6VwxCYhDR8lPcq3+EjOS8iMxO3jnIqY2qawOMYE4iofY/wUv/uMWP5z+A5YE8fjjvrVV1kGCLK/1OBAfcnA2OktD3OrwNmz9kPulwn5f+YHyLQHCZHsbyovrGwOahN/qV1I+/zMAiRsevrI67JiOerAf6efQCvtPKmi8ZvM6YMksjbwZvQqiANi+dOMk5W7zr0GVmO2QiZ+5gFRB/Nr486jo+Xm0/ZZwzlj0=
        |
      """.stripMargin
    val ymlKeys: Microgrammar = {
      val key: Matcher = Regex("[A-Za-z_]+", Some("key"))
      val value = Regex("[A-Za-z0-9\\-]+", Some("value"))
      val pair = "-" ~? key ~? Alternate(":", "=") ~? value
      val envList = "env:" ~~ "global:" ~~ Repsep(pair, WhitespaceOrNewLine, None) //"keys")
      new MatcherMicrogrammar(envList)

    }
    val m = MatcherMicrogrammarTest.readyMatchesFromString(ymlKeys.findMatches(input), input)
    assert(m.size === 1)
    //    println(TreeNodeUtils.toShorterString(m.head))
    //    println(s"Has ${m.head.fieldValues.size} field values: ${m.head.fieldValues}")
    withClue(s"Didn't expect to find match ${TreeNodeUtils.toShorterString(m.head)} with ${m.head.childNodes.size} children and fields=${m.head.childNodes}\n") {
      val keys = m.head.childrenNamed("key")
      val values = m.head.childrenNamed("value")
      assert(keys.size === 2)
      val k1 = keys.head
      assert(k1.value === "CI_DEPLOY_USERNAME")
      assert(values.head.value === "travis-mvn-deploy")
    }
  }

  private val printlns: MatcherMicrogrammar = {
    val printlns = "println(" ~ Break(")", Some("content"))
    new MatcherMicrogrammar(printlns)

  }

  it should "use println matcher directory" in {
    val p1 = "println(\"The first thing\")"
    val input =
      s"""$p1
         |
         |   println("The second thing")
         |
         |   println(s"And this");
         |
         |}
      """.stripMargin
    printlns.matcher.matchPrefix(InputState(input)) match {
      case Right(pm) =>
        assert(pm.matched === p1)
        val positionedNode = pm.node
        val (hatched, _) = PositionedMutableContainerTreeNode.pad(positionedNode, input)
        p1.contains(hatched.value) should be(true)
      case Left(boo) => fail(DismatchReport.detailedReport(boo, input))
    }
  }

  it should "find simple printlns in Java" in {
    val p1 = "println(\"The first thing\")"
    val input =
      s"""
         |class Foo {
         |
         |   $p1
         |
         |   println("The second thing")
         |
         |   println(s"And this");
         |
         |}
         |""".stripMargin
    val m = readyMatchesFromString(printlns.findMatches(input), input)
    assert(m.size === 3)
    //println(TreeNodeUtils.toShortString(m.head))
    assert(m.head.value === p1)
    val newThing = s"""/* $p1 */"""
    m.head.update(newThing)
    assert(m.head.value === newThing)
  }

  protected def thingGrammar: MatcherMicrogrammar = {
    val matcher = Regex(".*", Some("thing"))
    new MatcherMicrogrammar(matcher)

  }

  protected def matchPrivateJavaFields: Microgrammar = {
    val field = "private" ~~ Regex("[a-zA-Z0-9]+", Some("type")) ~~ Regex("[a-zA-Z0-9]+", Some("name"))
    new MatcherMicrogrammar(field)

  }

  protected def aWasaB: MatcherMicrogrammar =
    new MatcherMicrogrammar(
      Regex("[A-Z][a-z]+", Some("name")) ~? Literal("was aged") ~? Regex("[0-9]+", Some("age"))
    )

  protected def matchAnnotatedJavaFields: Microgrammar = {
    val visibility: Matcher = "public" | "private"
    val annotation = "@" ~ Regex("[a-zA-Z0-9]+", Some("annotationType"))
    val field = annotation ~~ visibility ~~ Regex("[a-zA-Z0-9]+", Some("type")) ~~ Regex("[a-zA-Z0-9]+", Some("name"))
    new MatcherMicrogrammar(field)
  }
}

class RepMatcherTest extends FlatSpec with Matchers {

  it should "handle simple rep" in {
    val repTest: Microgrammar = {
      val key: Matcher = Regex("[A-Za-z_]+,", Some("key"))
      val sentence: Matcher = Literal("keys:", Some("prefix")) ~? Rep(key, None) //"keys")
      new MatcherMicrogrammar(sentence)

    }
    val input =
      """
        |keys: a,b,cde,f,
        |And this is unrelated
        |   bollocks
        |      keys: x,y,
      """.stripMargin
    val m = MatcherMicrogrammarTest.readyMatchesFromString(repTest.findMatches(input), input)
    assert(m.size === 2)
    val firstMatch = m.head
    //println(TreeNodeUtils.toShortString(firstMatch))
    //println(s"First match in its entirely was [${firstMatch.value}]")
    //println(s"First match=\n${TreeNodeUtils.toShortString(firstMatch)}")
    //println(s"The child names under firstMatch are ${firstMatch.childNodeNames.mkString(",")}")
    val keys: Seq[TreeNode] = m.head.childrenNamed("key")
    withClue(s"Didn't expect ${TreeNodeUtils.toShorterString(m.head)}") {
      assert(keys.size === 4)
      keys.map(k => k.value) should equal(Seq("a,", "b,", "cde,", "f,"))
    }
  }

  it should "handle simple rep with wrap" in {

    val repTest: Microgrammar = {
      val key: Matcher = Regex("[A-Za-z_]+,", Some("key"))
      val sentence: Matcher = Literal("keys:", Some("prefix")) ~? Wrap(Rep(key, None) /*"key")*/ , "feet")
      new MatcherMicrogrammar(sentence, "findKeys")

    }
    val input =
      """
        |keys: a,b,cde,f,
        |And this is unrelated
        |   bollocks
        |      keys: x,y,
      """.stripMargin
    val m = MatcherMicrogrammarTest.readyMatchesFromString(repTest.findMatches(input), input)
    assert(m.size === 2)
    val firstMatch = m.head
    val feet = firstMatch.childrenNamed("feet")
    assert(feet.size === 1)
    val keys = feet.head.childrenNamed("key")
    assert(keys.size === 4)
    keys.map(k => k.value) should equal(Seq("a,", "b,", "cde,", "f,"))
  }

  it should "handle simple repsep" in {
    val repsep: Microgrammar = {
      val key: Matcher = Regex("[A-Za-z_]+", Some("key"))
      val sentence: Matcher = Literal("keys:", Some("prefix")) ~? Repsep(key, Literal(","), None) //keys
      new MatcherMicrogrammar(sentence)

    }
    val input =
      """
        |keys: a,b,cde,f
        |And this is unrelated
        |   bollocks
        |      keys: x,y
      """.stripMargin
    val m = MatcherMicrogrammarTest.readyMatchesFromString(repsep.findMatches(input), input)
    assert(m.size === 2)
    //println(s"Final match=\n${TreeNodeUtils.toShortString(m.head)}")
    //     m.head.childrenNamed("keys").size should be(1)
    val keys: Seq[TreeNode] = m.head.childrenNamed("key") //.head.asInstanceOf[ContainerTreeNode].childrenNamed("key")
    assert(keys.size === 4)
    keys.map(k => k.value) should equal(Seq("a", "b", "cde", "f"))
  }
}
