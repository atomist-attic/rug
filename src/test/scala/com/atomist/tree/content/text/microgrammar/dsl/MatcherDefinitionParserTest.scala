package com.atomist.tree.content.text.microgrammar.dsl

import com.atomist.rug.BadRugException
import com.atomist.tree.content.text.microgrammar._
import org.scalatest.{FlatSpec, Matchers}

class MatcherDefinitionParserTest extends FlatSpec with Matchers {

  val mgp = new MatcherDefinitionParser

  import MatcherDefinitionParser._

  it should "reject null string" in {
    val bogusInputs = Seq(null, "", "$", "[", "▶")
    for (bad <- bogusInputs)
      withClue(s"[$bad] is not a valid microgrammar definition") {
        an[BadRugException] should be thrownBy mgp.parseMatcher("x", bad)
      }
  }

  it should "accept valid literal" in {
    val validLiterals = Seq("a", "aa", "a&a", "woiurwieur", "def")
    for (v <- validLiterals) mgp.parseMatcher("v", v) match {
      case Literal(`v`, None) =>
    }
  }

  it should "accept valid regex" in {
    val validLiterals = Seq(
      s"${RegexpOpenToken}a$RegexpCloseToken",
      s"$RegexpOpenToken[.*]$RegexpCloseToken",
      s"$RegexpOpenToken[.]$RegexpCloseToken")
    for (v <- validLiterals) mgp.parseMatcher("y", v) match {
      case Regex("y", rex, _) =>
    }
  }

  it should "accept valid inline regex" in {
    val validLiterals = Seq(
      s"${VariableDeclarationToken}foo:${RegexpOpenToken}a$RegexpCloseToken",
      s"${VariableDeclarationToken}foo:$RegexpOpenToken.*$RegexpCloseToken",
      s"${VariableDeclarationToken}foo:$RegexpOpenToken.$RegexpCloseToken")
    for (v <- validLiterals) mgp.parseMatcher("x", v) match {
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
    for (v <- validDescendantPhrases) mgp.parseMatcher("v", v, mr) match {
      case w: Wrap =>
    }
  }

  it should "accept valid descendant phrase with predicate" in {
    val dog = Literal("dog", named = Some("dog"))
    val cat = Literal("cat", named = Some("Cat"))
    val mr = SimpleMatcherRegistry(Seq(dog,cat))
    val validDescendantPhrases = Seq("▶$fido:dog[curlyDepth=3]", "▶$felix:Cat[fat=false]")
    for (v <- validDescendantPhrases) mgp.parseMatcher("v", v, mr) match {
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
    for (v <- validUseOfVars) mgp.parseMatcher("t", v, mr) match {
      case m: Matcher =>
    }
  }

  it should "reject valid unbound $ variables" in {
    val validUseOfVars = Seq("$:OtherPattern", "def $:ScalaMethod", "def $theMethod:ScalaMethod")
    for (v <- validUseOfVars) {
      withClue(s"[$v] is not a bound valid microgrammar definition") {
        an[BadRugException] should be thrownBy mgp.parseMatcher("v", v)
      }
    }
  }

  it should "reject invalid $ variables" in {
    val bogusInputs = Seq("$", "$$", "$***", "$7iud:eiruieur", "$ foo:bar")
    for (bad <- bogusInputs)
      withClue(s"[$bad] is not a valid microgrammar definition") {
        an[BadRugException] should be thrownBy mgp.parseMatcher("bad", bad)
      }
  }

  it should "accept valid phrase of literals" in {
    val validLiterals = Seq("a a", "aa bb", "a &a", "one two three four", "def f = { \"blah\" }")
    for (v <- validLiterals) {
      withClue(s"[$v] IS a valid microgrammar definition") {
        mgp.parseMatcher("x", v) match {
          case cat: Concat =>
            cat.matchPrefix(0, v) match {
              case Some(PatternMatch(_, _, matched, `v`, _)) =>
              case None => fail(s"Failed to match on [$v]")
            }
        }
      }
    }
  }

  it should "accept valid break alone" in {
    val f = s"""$BreakOpenToken<span data-original="$BreakCloseToken"""
    mgp.parseMatcher("f", f) match {
      case x =>
    }
  }

  it should "accept valid break in string" in {
    val f = s"""<tr class="emoji_row">$BreakOpenToken<span data-original="${BreakCloseToken}and now for something completely different"""
    mgp.parseMatcher("f", f) match {
      case x : Matcher =>
        val matchThisYouMicrogrammar = x.matchPrefix(0, """<tr class="emoji_row">THIS OTHER STUFF<span data-original="and now for something completely different blah blah more things here""")
        matchThisYouMicrogrammar.isDefined should be(true)
    }
  }
}
