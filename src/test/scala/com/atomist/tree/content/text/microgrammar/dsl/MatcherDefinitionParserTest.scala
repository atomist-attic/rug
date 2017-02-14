package com.atomist.tree.content.text.microgrammar.dsl

import com.atomist.rug.BadRugException
import com.atomist.tree.content.text.microgrammar._
import org.scalatest.{FlatSpec, Matchers}

class MatcherDefinitionParserTest extends FlatSpec with Matchers {

  val mgp = new MatcherDefinitionParser

  import MatcherDefinitionParser._

  it should "reject null string" in {
    val bogusInputs = Seq(null, "", "$", "[")
    for (bad <- bogusInputs)
      withClue(s"[$bad] is not a valid microgrammar definition") {
        an[BadRugException] should be thrownBy mgp.parseMatcher("x", bad)
      }
  }

  it should "accept valid literal" in {
    val validLiterals = Seq("a", "aa", "a&a", "woiurwieur", "def")
    for {v <- validLiterals
         m = mgp.parseMatcher("a_nice_literal", v)
    } {
      assert(m.name === "a_nice_literal")
      assert(m.matchPrefix(InputState(v)).isRight)
      assert(m.matchPrefix(InputState("something else")).isLeft)
    }
  }

  it should "accept valid regex" in {
    val validLiterals = Seq(
      s"${RegexpOpenToken}y$RegexpCloseToken",
      s"$RegexpOpenToken.*$RegexpCloseToken",
      s"$RegexpOpenToken[.y]$RegexpCloseToken")
    val matchingInput = InputState("y")
    for {v <- validLiterals
         m = mgp.parseMatcher("y", v)}  {
      assert(m.name === "y")
      assert(m.matchPrefix(matchingInput).isRight, s"${matchingInput.input} didn't match $v")
    }
  }

  it should "accept valid inline regex" in {
    val validLiteralsWithMatchingAndUnmatchinExamples = Seq(
      (s"${VariableDeclarationToken}foo:${RegexpOpenToken}a$RegexpCloseToken", "alala", "bababoo"),
      (s"${VariableDeclarationToken}foo:$RegexpOpenToken.*f$RegexpCloseToken", "oh look i have an f in me", "none here"),
      (s"${VariableDeclarationToken}foo:$RegexpOpenToken^.$$$RegexpCloseToken", "a", "aa"))
    for {(v, matches, matchesNot) <- validLiteralsWithMatchingAndUnmatchinExamples
         m = mgp.parseMatcher("x", v)}
    {
      assert(m.name === "x")
      val matchingIS = InputState(matches)
      assert(m.matchPrefix(matchingIS).isRight, s"$v did not match $matches")
      assert(m.matchPrefix(InputState(matchesNot)).isLeft)
    }
  }

  it should "reject invalid $ variables" in {
    val bogusInputs = Seq("$", "$$", "$***", "$7iud:eiruieur", "$ foo:bar")

    for (bad <- bogusInputs)
      withClue(s"[$bad] is not a valid microgrammar definition") {
        an[BadRugException] should be thrownBy {
          val m = mgp.parseMatcher("bad", bad)
          println(s"Here is the unexpected matcher: $m")
        }
      }
  }

  it should "accept valid phrase of literals" in {
    val validLiterals = Seq("a a", "aa bb", "a &a", "one two three four", "def f = { \"blah\" }")
    for (v <- validLiterals) {
      withClue(s"[$v] IS a valid microgrammar definition") {
        mgp.parseMatcher("x", v) match {
          case Wrap(cat: Concat, "x") =>
            cat.matchPrefix(InputState(v)) match {
              case Right(PatternMatch(_, matched, InputState2(`v`, _, _), _)) =>
              case Left(report) => fail(s"Failed to match on [$v]" + report)
              case _ => fail(s"failed to parse/match $v")
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

  it should "accept short break in string" in {
    val f = s"""<div id="$BreakOpenToken"${BreakCloseToken}"""
    mgp.parseMatcher("f", f) match {
      case parsedMatcher: Matcher =>
        val validInputs = Seq(
          """<div id="foo" """,
          """<div          id="fom o" """,
          """<div id="2394029384  %^34o5u345ewjh029384xxxfd!" """
        )
        validInputs.foreach { in =>
          parsedMatcher.matchPrefix(
            InputState(in)) match {
            case Right(pe) =>
            //pe.matched should be ("foo\"")
            case Left(report) => fail(s"[$in] didn't match and should have done: " + report)
            case _ => ???
          }
        }
    }
  }

  it should "accept valid break in string" in {
    val f = s"""<tr class="emoji_row">$BreakOpenToken<span data-original="${BreakCloseToken}and now for something completely different"""
    mgp.parseMatcher("f", f) match {
      case parsedMatcher: Matcher =>
        parsedMatcher.matchPrefix(
          InputState("""<tr class="emoji_row">THIS OTHER STUFF<span data-original="and now for something completely different""")) match {
          case Right(pe) =>
          case _ => ???
        }
    }
  }

  it should "parse strict string literals" in {
    val f = s"""${StrictLiteralOpen}xxxx$StrictLiteralClose"""
    val parsed = mgp.parseMatcher("f", f)
    parsed.name should be("f")
    val matchingInput = InputState("xxxx")
    val notMatchingInput = InputState(" xxxx")
    assert(parsed.matchPrefix(matchingInput).isRight)
    assert(parsed.matchPrefix(notMatchingInput).isLeft)
  }

  it should "accept valid break in string using strict literals" in {
    val f = s"""$StrictLiteralOpen<tr class="emoji_row">$StrictLiteralClose$BreakOpenToken<span data-original="${BreakCloseToken}"""
    mgp.parseMatcher("f", f) match {
      case parsedMatcher: Matcher =>
        parsedMatcher.matchPrefix(
          InputState("""<tr class="emoji_row">THIS OTHER STUFF<span data-original="and now for something completely different""")) match {
          case Right(pe) =>
          case _ => ???
        }
    }
  }

  it should "accept valid break in string using strict literals with suffix and post-suffix" in {
    val suffixes = Seq(
      "",
      "short",
      "and now for something completely different"
    )
    val postsuffixes = Seq(
      "",
      "x"
    )
    for {
      suffix <- suffixes
      postsuffix <- postsuffixes
    } {
      val f = s"""$StrictLiteralOpen<tr class="emoji_row">$StrictLiteralClose$BreakOpenToken<span data-original="${BreakCloseToken}${StrictLiteralOpen}$suffix${StrictLiteralClose}$postsuffix"""
      mgp.parseMatcher("f", f) match {
        case parsedMatcher: Matcher =>
          parsedMatcher.matchPrefix(
            InputState(s"""<tr class="emoji_row">THIS OTHER STUFF<span data-original="${suffix}$postsuffix""")) match {
            case Right(pe) =>
            case Left(report) => fail(s"Expected to match with suffix of [$suffix] and postsuffix of [$postsuffix] but did not: " + report)
            case _ => ???
          }
      }
    }
  }

  it should "accept literals and variables with whitespace before and after" in {
    val input =
      """
      |<parent>
      |<groupId>$groupId</groupId>
      |<artifactId>$artifactId</artifactId>
      |<version>$version</version>
      |</parent>
      |""".stripMargin

    mgp.parseMatcher("Martha", input)
  }

}
