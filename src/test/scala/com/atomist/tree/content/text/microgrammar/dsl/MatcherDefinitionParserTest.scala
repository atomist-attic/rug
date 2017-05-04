package com.atomist.tree.content.text.microgrammar.dsl

import com.atomist.rug.BadRugException
import com.atomist.tree.content.text.microgrammar._
import org.scalatest.{FlatSpec, Matchers}

class MatcherDefinitionParserTest extends FlatSpec with Matchers {

  val mgp = new MatcherDefinitionParser

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

  it should "accept literals and variables with whitespace before and after" in {
    val input =
      """
      |<parent>
      |<groupId>$groupId</groupId>
      |</parent>
      |""".stripMargin

    val m = mgp.parseMatcher("Martha", input)

    val matchingInputWithWhitespace = " <parent> <groupId>whatever</groupId> </parent>"
    val km = Map("groupId" -> Regex("[a-z]+"))
    val res = m.matchPrefix(InputState(matchingInputWithWhitespace, knownMatchers = km))

    res match {
      case Right(r) => //yay
      case Left(dr) =>
        println(m.shortDescription(km))
        println(DismatchReport.detailedReport(dr, matchingInputWithWhitespace))
        fail("That should match")
    }

  }

}
