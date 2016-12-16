package com.atomist.rug.kind.grammar

import com.atomist.tree.content.text._
import com.atomist.tree.content.text.grammar.{AbstractMatchListener, MatchListener, PositionalString}
import com.atomist.tree.content.text.microgrammar._
import com.atomist.tree.utils.TreeNodeUtils
import com.atomist.tree.{ContainerTreeNode, SimpleTerminalTreeNode, TerminalTreeNode, TreeNode}
import org.scalatest.{FlatSpec, Matchers}

abstract class MicrogrammarTest extends FlatSpec with Matchers {

  protected def thingGrammar: Microgrammar

  it should "parse and update complete match" in {
    val g = thingGrammar
    val input = "This is a test"
    val m = g.strictMatch(input)
    m.count should be >= (1)
    m("thing").head match {
      case sm: MutableTerminalTreeNode =>
        sm.nodeName should equal("thing")
        sm.value should equal(input)
        val newContent = "This is the new content"
        sm.update(newContent)
        sm.dirty should be(true)
        m.dirty should be(true)
        m.value should equal(newContent)
    }
  }

  protected def aWasaB: Microgrammar

  it should "parse 1 match of 2 parts in whole string" in {
    val matches = aWasaB.strictMatch("Henry was aged 19")
    matches.count should be >= (2)
    matches("name").head match {
      case sm: MutableTerminalTreeNode =>
        sm.value should equal("Henry")
    }
    matches("age").head match {
      case sm: MutableTerminalTreeNode =>
        sm.value should equal("19")
    }
  }

  it should "parse 1 match of 2 parts in whole string and replace both keys" in {
    val g = aWasaB
    val input = "Henry was aged 19"
    val matches = g.strictMatch(input)
    matches.value should equal(input)
    matches.count should be >= (2)
    matches.dirty should be(false)
    matches("name").head match {
      case sm: MutableTerminalTreeNode =>
        sm.value should equal("Henry")
        sm.update("Dave")
    }
    matches("age").head match {
      case sm: MutableTerminalTreeNode =>
        sm.value should equal("19")
        sm.update("40")
    }
    matches.dirty should be(true)
    val currentContent = matches.value
    currentContent should be("Dave was aged 40")
  }

  it should "parse 1 match of 2 parts discarding prefix" in {
    val g = aWasaB
    val input = "rubbish goes here. Henry was aged 12"
    val m = g.findMatches(input)
    m.size should be(1)
    m.head.count should be >= (2)
    m.head("name").head match {
      case sm: MutableTerminalTreeNode =>
        sm.value should equal("Henry")
    }
    m.head("age").head match {
      case sm: MutableTerminalTreeNode =>
        sm.value should equal("12")
    }
  }

  it should "parse 1 match of 2 parts discarding suffix" in {
    val g = aWasaB
    val m = g.findMatches("Henry was aged 24. Blah blah")
    m.size should be(1)
    m.head.count should be >= (2)
    m.head("name").head match {
      case sm: MutableTerminalTreeNode =>
        sm.value should equal("Henry")
    }
    m.head("age").head match {
      case sm: MutableTerminalTreeNode =>
        sm.value should equal("24")
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
    val m = g.findMatches(input)
    m.size should be(2)
    m.head.count should be >= (2)
    val t = m(1)
    t("name").head match {
      case sm: MutableTerminalTreeNode =>
        sm.value should equal("Alice")
        sm.startPosition != null should be(true)
    }
    t("age").head match {
      case sm: MutableTerminalTreeNode =>
        sm.value should equal("16")
    }
  }

  protected def matchScalaMethodHeaderRepsep: Microgrammar

  it should "parse 1 scala method without parameters" in {
    val g = matchScalaMethodHeaderRepsep
    val input =
      """
        |def foo(): Int = {
        | 0
        |}
      """.stripMargin
    val m = g.findMatches(input)
    m.size should be(1)
    m.head("name").head match {
      case sm: MutableTerminalTreeNode =>
        sm.value should equal("foo")
    }
    m.head("type").head match {
      case sm: MutableTerminalTreeNode =>
        sm.value should equal("Int")
    }
  }

  it should "parse 1 scala method with repsep parameters" in
    matchScalaMethodHeaderUsing(matchScalaMethodHeaderRepsep)

  private def matchScalaMethodHeaderUsing(mg: Microgrammar, ml: Option[MatchListener] = None) {
    val input =
      """
        |def bar(barParamName: String): Unit = {
        |}
      """.stripMargin
    val m = mg.findMatches(input, ml)
    if (ml.isDefined) ml.get.matches should equal(1)
    m.size should be(1)
    println(TreeNodeUtils.toShortString(m.head))
    m.head("name").head match {
      case sm: MutableTerminalTreeNode =>
        sm.value should equal("bar")
    }
    m.head("type").head match {
      case sm: MutableTerminalTreeNode =>
        sm.value should equal("Unit")
    }
    val params = m.head("params")
    val paramDef1 = params.head.asInstanceOf[ContainerTreeNode]("param_def")
    paramDef1.size should be(1)
    paramDef1.head match {
      case ov: ContainerTreeNode =>
        ov("name").toList match {
          case (fv: TerminalTreeNode) :: Nil =>
            fv.nodeName should equal("name")
            fv.value should equal("barParamName")
        }
        ov("type").toList match {
          case (fv: TerminalTreeNode) :: Nil =>
            fv.nodeName should equal("type")
            fv.value should equal("String")
        }
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
    val m = matchScalaMethodHeaderRepsep.findMatches(input)
    m.size should be(1)
    val last: MutableContainerTreeNode = m.head
    val params = last("params").head.asInstanceOf[ContainerTreeNode]("param_def")
    params.size should be >= (2)
    params foreach {
      case pad: SimpleTerminalTreeNode => pad.nodeName.contains("pad") should be(true)
      case soo: MutableContainerTreeNode =>
        soo("type").head.asInstanceOf[TerminalTreeNode].value should be("Int")
        val value = soo("name").head.asInstanceOf[TerminalTreeNode].value
        Set("a", "b").contains(value) should be(true)
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
        | def other(a: Int,${pad1}b${pad1}:${pad2}Int): Int = a + b
        |}
      """.stripMargin
    val m = matchScalaMethodHeaderRepsep.findMatches(input)
    m.size should be(3)
    m.head("name").head match {
      case sm: MutableTerminalTreeNode =>
        sm.value should equal("bar")
    }
    m.head("type").head match {
      case sm: MutableTerminalTreeNode =>
        sm.value should equal("Unit")
    }
    val last: MutableContainerTreeNode = m(2)
    val params = last("params").head.asInstanceOf[ContainerTreeNode]("param_def")
    params.size should be >= (2)
    params foreach {
      case pad: SimpleTerminalTreeNode => pad.nodeName.contains("pad") should be(true)
      case soo: MutableContainerTreeNode =>
        soo("type").head.asInstanceOf[TerminalTreeNode].value should be("Int")
        val value = soo("name").head.asInstanceOf[TerminalTreeNode].value
        Set("a", "b").contains(value) should be(true)
    }
  }

  protected def matchAnnotatedJavaFields: Microgrammar

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
    val m = g.findMatches(input)
    m.size should be(2)
    m.head("name").head match {
      case sm: MutableTerminalTreeNode =>
        sm.value should equal("hippo")
    }
    m.head("type").head match {
      case sm: MutableTerminalTreeNode =>
        sm.value should equal("Hippopotamus")
    }
  }

  protected def matchPrivateJavaFields: Microgrammar

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
    val m = matchAnnotatedJavaFields.findMatches(input)
    m.size should be(2)
    m.head("name").head match {
      case sm: MutableTerminalTreeNode =>
        sm.value should equal("hippo")
    }
    m.head("type").head match {
      case sm: MutableTerminalTreeNode =>
        sm.value should equal("Hippopotamus")
    }
    val m2 = matchPrivateJavaFields.findMatches(input)
    m2.size should be(2)
  }

  class SavingMatchListener extends AbstractMatchListener("test") {
    var hits: List[ContainerTreeNode] = Nil
    var skipped: List[PositionalString] = Nil

    override protected def onMatchInternal(m: ContainerTreeNode): Unit = {
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
    val m = g.findMatches(input, Some(ml))
    m.size should be(2)
    m.head("name").head match {
      case sm: MutableTerminalTreeNode =>
        sm.value should equal("hippo")
    }
    m.head("type").head match {
      case sm: MutableTerminalTreeNode =>
        sm.value should equal("Hippopotamus")
    }
    ml.matches should be(2)
    //ml.hits.foreach(_.asInstanceOf[MutableObjectFieldValueImpl].startPosition.asInstanceOf[LineInputPosition].lineFrom1 should be > (1))
    //ml.skipped.size should be > (0)
  }

  protected def ymlKeys: Microgrammar

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
    val m = ymlKeys.findMatches(input)
    m.size should be(1)
    //println(TreeNodeUtils.toShortString(m.head))
    val keys = m.head("key")
    val values = m.head("value")
    keys.size should be (2)
    val k1 = keys.head
    k1.value should equal("CI_DEPLOY_USERNAME")
    values.head.value should equal ("travis-mvn-deploy")
  }

  protected def repTest: Microgrammar

  it should "handle simple rep" in {
    val input =
      """
        |keys: a,b,cde,f,
        |And this is unrelated
        |   bollocks
        |      keys: x,y,
      """.stripMargin
    val m = repTest.findMatches(input)
    m.size should be(2)
    //println(s"Final match=\n${TreeNodeUtils.toShortString(m.head)}")
    m.head("keys").size should be (1)
    val keys: Seq[TreeNode] = m.head("keys").head.asInstanceOf[ContainerTreeNode]("key")
    keys.size should be (4)
    keys.map(k => k.value) should equal(Seq("a,", "b,", "cde,", "f,"))
  }

  protected def repsepTest: Microgrammar

  it should "handle simple repsep" in {
    val input =
      """
        |keys: a,b,cde,f
        |And this is unrelated
        |   bollocks
        |      keys: x,y
      """.stripMargin
    val m = repsepTest.findMatches(input)
    m.size should be(2)
    //println(s"Final match=\n${TreeNodeUtils.toShortString(m.head)}")
    m.head("keys").size should be (1)
    val keys: Seq[TreeNode] = m.head("keys").head.asInstanceOf[ContainerTreeNode]("key")
    keys.size should be (4)
    keys.map(k => k.value) should equal(Seq("a", "b", "cde", "f"))
  }
}