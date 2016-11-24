package com.atomist.model.content.grammar.microgrammar

import com.atomist.model.content.text._
import com.atomist.rug.RugRuntimeException
import com.atomist.util.ConsoleVisitor
import org.scalatest.{FlatSpec, Matchers}

class AntlrParserTest extends FlatSpec with Matchers {

  private def createMicrogrammar(g4: String, name: String): Microgrammar = {
    new InMemAntlrMicrogrammar(g4, name)
  }

  it should "provide informative error message on grammar without ;" in {
    val badG4 =
      s"""
         |INT : [0-9]+
         |intr: INT;
       """.stripMargin

    try {
      InMemAntlrMicrogrammar.lastProduction(badG4, "intr")
      fail
    } catch {
      case rre: RugRuntimeException =>
        rre.getMessage.contains("unterminated rule") should be(true)
    }
  }

  it should "match regex expansion" in {
    val re = "[0-9]+"
    val antlr =
      s"""
         |INT : $re;
         |intr: INT;
       """.stripMargin

    val mg = createMicrogrammar(antlr, "intr")
    val m = mg.strictMatch("23")
    m.nodeName should equal("intr")
    m.childNodes.size should be >= (1)
    m.childNodes.head.asInstanceOf[MutableTerminalTreeNode].value should equal("23")
  }

  it should "match regex expansion with alias" in
    testAlias("val")

  it should "match regex expansion with camel case alias" in
    testAlias("numericValue")

  // TODO test failure conditions

  private def testAlias(alias: String) {
    val re = "[0-9]+"
    val g4 =
      s"""
         |INT : $re;
         |expr : $alias=INT;
       """.stripMargin

    val mg = createMicrogrammar(g4, "expr")
    val m = mg.strictMatch("23")
    m.childNodes.size should be >= (1)
    m(alias).head.asInstanceOf[MutableTerminalTreeNode].value should equal("23")
  }

  it should "match regex lookup" in pendingUntilFixed {
    val g4 =
      """
        |method_name : methodName=java_identifier;
      """.stripMargin

    val expected = "theDogAteMyHomework"
    val mg = createMicrogrammar(g4, "method_name")

    val m = mg.strictMatch(expected)
    m.nodeName should equal("methodName")
    m.childNodes.size should be(1)
    m.childNodes.head.asInstanceOf[MutableTerminalTreeNode].value should equal(expected)
  }

  it should "match simple reference" in {
    val g4 =
      """
        |TERM : [0-9]+;
        |expr : t=TERM;
      """.stripMargin

    val ug = createMicrogrammar(g4, "expr")

    val m = ug.strictMatch("23")
    m.childNodes.size should be >= (1)
    m("t") match {
      case Seq(sm: MutableTerminalTreeNode) =>
        sm.nodeName should equal("t")
        sm.value should equal("23")
    }
  }

  it should "match concatenation" in {
    val g4 =
      """
        |NAME : [a-z]+;
        |VALUE : [0-9]+;
        |EQ: '=';
        |expr : name=NAME EQ value=VALUE;
      """.stripMargin

    val ug = createMicrogrammar(g4, "expr")
    val m = ug.strictMatch("x = 23")
    m.childNodes.size should be >= (2)
    m("name") match {
      case Seq(sm: MutableTerminalTreeNode) =>
        sm.nodeName should equal("name")
        sm.value should equal("x")
    }
    m("value") match {
      case Seq(sm: MutableTerminalTreeNode) =>
        sm.nodeName should equal("value")
        sm.value should equal("23")
    }
  }

  it should "match concatenation in multiple matches" in {
    val g4 =
      """
        |NAME : [A-Z][a-z]+;
        |VALUE : [0-9]+;
        |EQ: '=';
        |expr : name=NAME EQ value=VALUE;
      """.stripMargin

    val ug = createMicrogrammar(g4, "expr")
    val ms = ug.findMatches(
      """
        |Margaret = 0,
        |Tony = 1
        |and we can safely ignore this
        |and Gordon = 2
      """.stripMargin)
    ms.size should be(3)
    val m = ms(0)
    m("name") match {
      case Seq(sm: MutableTerminalTreeNode) =>
        sm.nodeName should equal("name")
        sm.value should equal("Margaret")
    }
    m("value") match {
      case Seq(sm: MutableTerminalTreeNode) =>
        sm.nodeName should equal("value")
        sm.value should equal("0")
    }
    val m2 = ms(2)
    m2("name") match {
      case Seq(sm: MutableTerminalTreeNode) =>
        sm.nodeName should equal("name")
        sm.value should equal("Gordon")
    }
    m2("value") match {
      case Seq(sm: MutableTerminalTreeNode) =>
        sm.nodeName should equal("value")
        sm.value should equal("2")
    }
  }

  it should "match concatenation with list binding" in {
    val g4 =
      """
        |NAME : [A-Z][a-z]+;
        |sentence : 'Names:' names+=NAME 'and' names+=NAME;
      """.stripMargin

    val ug = createMicrogrammar(g4, "sentence")

    val m = ug.strictMatch("Names: Ted and George")
    m("names").size should be >= (2)
    m("names").collect {
      case sf: MutableTerminalTreeNode => sf.value
    } should equal(Seq("Ted", "George"))

  }

  it should "match nested list binding" in {
    val g4 =
      """
        |NAME : [A-Z][a-z]+;
        |athletes: '-athletes:' name+=NAME+;
        |spectators: '-spectators:' name+=NAME+;
        |sentence : athletes spectators;
      """.stripMargin

    val ug = createMicrogrammar(g4, "sentence")
    val input =
      """
        |-athletes: Bolt Gatlin
        |-spectators: Craven Chiller
      """.stripMargin

    for {
      m <- Seq(
        ug.strictMatch(input), {
          val ms = ug.findMatches(input)
          ms.size should be(1)
          ms.head
        }
      )
    } {
      m("athletes").head.asInstanceOf[ContainerTreeNode].childNodes collect {
        case usf: MutableTerminalTreeNode => usf.value
      } should equal(Seq("Bolt", "Gatlin"))

      m("spectators").head.asInstanceOf[ContainerTreeNode].childNodes collect {
        case sf: MutableTerminalTreeNode => sf.value
      } should equal(Seq("Craven", "Chiller"))
    }
  }

  it should "match rep1" in
    matchRepetition(
      """
        | NAME : [a-z]+;
        | VALUE : [0-9]+;
        | EQ : '=';
        | expr : NAME EQ VALUE+;
        | """.stripMargin)

  it should "match rep1 with inline literal" in
    matchRepetition(
      """
        | NAME : [a-z]+;
        | VALUE : [0-9]+;
        | expr : NAME '=' VALUE+;""".stripMargin)

  it should "match rep" in
    matchRepetition(
      """
        | NAME : [a-z]+;
        | VALUE : [0-9]+;
        | EQ : '=';
        | expr : NAME EQ VALUE*;
      """.stripMargin)

  it should "match rep in parentheses" in
    matchRepetition(
      """
        | NAME : [a-z]+;
        | VALUE : [0-9]+;
        | EQ : '=';
        | expr : NAME EQ (VALUE)*;
      """.stripMargin)

  it should "match rep in parentheses with whitespace" in
    matchRepetition(
      """
        | NAME : [a-z]+;
        | VALUE : [0-9]+;
        | EQ : '=';
        | expr : NAME EQ (VALUE)*;
      """.stripMargin)

  // We need to do it this way as there's no rep sep production
  // See https://github.com/antlr/antlr4/blob/master/doc/parser-rules.md
  it should "match rep sep" in {
    val m = matchRepetition2(
      """
        | NAME : [a-z]+;
        | VALUE : [0-9]+;
        | values : vals+=VALUE (',' vals+=VALUE)* ;
        | expr : NAME '=' values;
      """.stripMargin, input = "x = 23,25,27"
    )
    val kids = m("values").head.asInstanceOf[ContainerTreeNode].childNodes collect {
      case sf: MutableTerminalTreeNode => sf
    }
    kids.size should be(3)
    kids(0).value should equal("23")
    kids(1).value should equal("25")
    kids(2).value should equal("27")
  }

  it should "match nesting" in {
    val m = matchRepetition2(
      """
        | NAME : [a-z]+;
        | VALUE : [0-9]+;
        | expr : NAME '=' VALUE+;
      """.stripMargin, input = "x = 23 25 27"
    )
    val kids = m("VALUE") collect {
      case sf: MutableTerminalTreeNode => sf
    }
    kids.size should be(3)
    kids(0).value should equal("23")
    kids(1).value should equal("25")
    kids(2).value should equal("27")
  }

  it should "handle C style comments" in {
    matchRepetition(
      """
        |/** Here's a good old fashioned
        |  C style comment
        |*/
        | NAME : [a-z]+;
        | VALUE : [0-9]+;
        | EQ : '=';
        | expr : NAME EQ VALUE*;
      """.stripMargin)
  }

  it should "match opt missing" in
    matchRepetition(
      """
        | NAME : [a-z]+;
        | VALUE : [0-9]+;
        | TERMINATOR :';';
        | expr : NAME '=' VALUE* TERMINATOR?;
      """.stripMargin, "x = 23 25")

  it should "match opt provided" in matchRepetition(
    """
      | NAME : [a-z]+;
      | VALUE : [0-9]+;
      | TERMINATOR :';';
      | EQ : '=';
      | expr : NAME EQ VALUE* TERMINATOR?;
    """.stripMargin, "x = 23 25;")

  private def matchRepetition(g4: String, input: String = "x = 23 25"): MutableContainerTreeNode = {
    val m = matchRepetition2(g4, input)
    val kids = m("VALUE") collect {
      case sf: MutableTerminalTreeNode => sf
    }
    kids.size should be(2)
    kids(0).value should equal("23")
    kids(1).value should equal("25")
    m
  }

  private def matchRepetition2(g4: String, input: String = "x = 23 25"): MutableContainerTreeNode = {
    val ug = createMicrogrammar(g4, "expr")
    val ms = ug.findMatches(input)
    ms.size should be(1)
    val m = ms.head
    m("NAME").headOption match {
      case Some(sm: MutableTerminalTreeNode) =>
        sm.nodeName should equal("NAME")
        sm.value should equal("x")
      case None =>
        m.accept(ConsoleVisitor, 0)
        fail(s"No field 'NAME' in $m")
    }
    m
  }

  it should "match OR" in {
    val g4 =
      """
        |NUM : [0-9]+;
        |WORD: 'word';
        |thing: NUM | WORD;
      """.stripMargin
    val ug = createMicrogrammar(g4, "thing")

    val m = ug.strictMatch("23")
    m.childNodes.size should be >= (1)
    m("NUM").headOption match {
      case Some(sm: MutableTerminalTreeNode) =>
        sm.value should equal("23")
    }

    val m2 = ug.strictMatch("word")
    m2.childNodes.size should be >= (1)
    m2("WORD").headOption match {
      case Some(sm: MutableTerminalTreeNode) =>
        sm.value should equal("word")
    }
  }
}
