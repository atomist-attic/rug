package com.atomist.tree.content.text.microgrammar.dsl

import com.atomist.rug.BadRugException
import com.atomist.tree.content.text.microgrammar._
import org.scalatest.{FlatSpec, Matchers}

class MatcherDefinitionParserTest extends FlatSpec with Matchers {

  val mgp = new MatcherDefinitionParser

  it should "reject null string" in {
    val bogusInputs = Seq(null, "", "$", "[", "▶")
    for (bad <- bogusInputs)
      withClue(s"[$bad] is not a valid microgrammar definition") {
        an[BadRugException] should be thrownBy mgp.parse(bad)
      }
  }

  it should "accept valid literal" in {
    val validLiterals = Seq("a", "aa", "a&a", "woiurwieur", "def")
    for (v <- validLiterals) mgp.parse(v) match {
      case Literal(`v`, None) =>
    }
  }

  it should "accept valid regex" in {
    val validLiterals = Seq("§a§", "§[.*]§", "§[.]§")
    for (v <- validLiterals) mgp.parse(v) match {
      case Regex(_, rex, _) =>
    }
  }

//  it should "parse regex in isolaion" in {
//    val l = "[0-]"
//    val mgp2 = new MatcherDSLDefinitionParser {
//      def p(s: String) = {
//        parseTo(StringFileArtifact("x", s), regex('[', ']'))
//      }
//    }
//    mgp2.p(l)
//  }

  it should "accept valid inline regex" in {
    val validLiterals = Seq("$foo:§a§", "$foo:§.*§", "$foo:§.§")
    for (v <- validLiterals) mgp.parse(v) match {
      case Regex("foo", rex, _) =>
        withClue(s"String [$v] should contain regex [$rex]") {
          v.contains(rex) should be (true) }
    }
  }

  it should "accept valid descendant phrase" in {
    val dog = Literal("dog", named = Some("dog"))
    val cat = Literal("cat", named = Some("cat"))
    val mr = SimpleMatcherRegistry(Seq(dog,cat))
    val validDescendantPhrases = Seq("▶$:cat", "▶$d:dog")
    for (v <- validDescendantPhrases) mgp.parse(v, mr) match {
      case w: Wrap =>
    }
  }

  it should "accept valid descendant phrase with predicate" in {
    val dog = Literal("dog", named = Some("dog"))
    val cat = Literal("cat", named = Some("Cat"))
    val mr = SimpleMatcherRegistry(Seq(dog,cat))
    val validDescendantPhrases = Seq("▶$fido:dog[curlyDepth=3]", "▶$felix:Cat[fat=false]")
    for (v <- validDescendantPhrases) mgp.parse(v, mr) match {
      //case Literal(l, None) =>
      case _ =>
    }
  }

  it should "accept valid bound $ variables" in {
    val otherPattern = Literal("x", named = Some("OtherPattern"))
    val scalaMethod = Literal("x", named = Some("ScalaMethod"))
    val theMethod = scalaMethod.copy(named = Some("theMethod"))

    val mr = SimpleMatcherRegistry(Seq(otherPattern, scalaMethod, theMethod))
    val validUseOfVars = Seq("$:OtherPattern", "def $:ScalaMethod", "def $theMethod:ScalaMethod")
    for (v <- validUseOfVars) mgp.parse(v, mr) match {
      case m: Matcher =>
    }
  }

  it should "reject valid unbound $ variables" in {
    val validUseOfVars = Seq("$:OtherPattern", "def $:ScalaMethod", "def $theMethod:ScalaMethod")
    for (v <- validUseOfVars) {
      withClue(s"[$v] is not a bound valid microgrammar definition") {
        an[BadRugException] should be thrownBy mgp.parse(v)
      }
    }
  }

  it should "reject invalid $ variables" in {
    val bogusInputs = Seq("$", "$$", "$***", "$7iud:eiruieur", "$ foo:bar")
    for (bad <- bogusInputs)
      withClue(s"[$bad] is not a valid microgrammar definition") {
        an[BadRugException] should be thrownBy mgp.parse(bad)
      }
  }

  it should "accept valid phrase of literals" in {
    val validLiterals = Seq("a a", "aa bb", "a &a", "one two three four", "def f = { \"blah\" }")
    for (v <- validLiterals) {
      withClue(s"[$v] IS a valid microgrammar definition") {
        mgp.parse(v) match {
          case cat: Concat =>
            cat.matchPrefix(0, v) match {
              case Some(PatternMatch(_, _, matched, `v`, _)) =>
              case None => fail(s"Failed to match on [$v]")
            }
        }
      }
    }
  }

  it should "parse multiple valid sentences" in {
    val validInputs = Seq(MicrogrammarDefinition("a", "a"), MicrogrammarDefinition("thing", "def foo"))
    val mr = mgp.parseInOrder(validInputs)
    mr.definitions.size should be (2)
    mr.find("a") should be(defined)
    mr.find("thing") should be (defined)
  }
}
