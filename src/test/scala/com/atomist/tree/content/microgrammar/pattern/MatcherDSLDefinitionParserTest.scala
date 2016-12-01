package com.atomist.tree.content.microgrammar.pattern

import com.atomist.rug.BadRugException
import com.atomist.tree.content.microgrammar._
import org.scalatest.{FlatSpec, Matchers}

class MatcherDSLDefinitionParserTest extends FlatSpec with Matchers {

  val mgp = new MatcherDSLDefinitionParser

  it should "reject null string" in {
    val bogusInputs = Seq(null, "", "$", "[", "▶")
    for (bad <- bogusInputs)
      withClue(s"[$bad] is not a valid microgrammar definition") {
        an[BadRugException] should be thrownBy (mgp.parse(bad))
      }
  }

  it should "accept valid literal" in {
    val validLiterals = Seq("a", "aa", "a&a", "woiurwieur", "def")
    for (v <- validLiterals) mgp.parse(v) match {
      case Literal(v, None) =>
    }
  }

  it should "accept valid descendant phrase" in pendingUntilFixed {
    val validDescendantPhrases = Seq("▶[foobar]", "▶[foo bar]")
    for (v <- validDescendantPhrases) mgp.parse(v) match {
      case Literal(v, None) =>
    }
  }

  it should "accept valid bound $ variables" in {
    val otherPattern = Literal("x", named = Some("OtherPattern"))
    val scalaMethod = Literal("x", named = Some("ScalaMethod"))
    val theMethod = scalaMethod.copy(named = Some("theMethod"))

    val mr = new SimpleMatcherRegistry(Seq(otherPattern, scalaMethod, theMethod))
    val validUseOfVars = Seq("$:OtherPattern", "def $:ScalaMethod", "def $theMethod:ScalaMethod")
    for (v <- validUseOfVars) mgp.parse(v, mr) match {
      case m: Matcher =>
    }
  }

  it should "reject valid unbound $ variables" in {
    val validUseOfVars = Seq("$:OtherPattern", "def $:ScalaMethod", "def $theMethod:ScalaMethod")
    for (v <- validUseOfVars) {
      withClue(s"[$v] is not a bound valid microgrammar definition") {
        an[BadRugException] should be thrownBy(mgp.parse(v))
      }
    }
  }

  it should "reject invalid $ variables" in {
    val bogusInputs = Seq("$", "$$", "$***", "$7iud:eiruieur", "$ foo:bar")
    for (bad <- bogusInputs)
      withClue(s"[$bad] is not a valid microgrammar definition") {
        an[BadRugException] should be thrownBy (mgp.parse(bad))
      }
  }

  it should "accept valid phrase of literals" in {
    val validLiterals = Seq("a a", "aa bb", "a &a", "one two three four", "def f = { \"blah\" }")
    for (v <- validLiterals) {
      withClue(s"[$v] IS a valid microgrammar definition") {
        mgp.parse(v) match {
          case cat: Concat =>
            cat.matchPrefix(0, v) match {
              case Some(PatternMatch(_, _, v, `v`, _)) =>
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
