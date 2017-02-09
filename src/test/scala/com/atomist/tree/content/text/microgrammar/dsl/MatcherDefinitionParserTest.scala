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
      
      case _ => fail(s"failed to parse $v")
    }
  }

  it should "accept valid regex" in {
    val validLiterals = Seq(
      s"${RegexpOpenToken}y$RegexpCloseToken",
      s"$RegexpOpenToken[.*]$RegexpCloseToken",
      s"$RegexpOpenToken[.]$RegexpCloseToken")
    for (v <- validLiterals) mgp.parseMatcher("y", v) match {
      case Regex(_, Some("y"), _) =>
      
      case _ => fail(s"failed to parse $v")
    }
  }

  it should "accept valid inline regex" in {
    val validLiterals = Seq(
      s"${VariableDeclarationToken}foo:${RegexpOpenToken}a$RegexpCloseToken",
      s"${VariableDeclarationToken}foo:$RegexpOpenToken.*$RegexpCloseToken",
      s"${VariableDeclarationToken}foo:$RegexpOpenToken.$RegexpCloseToken")
    for (v <- validLiterals) mgp.parseMatcher("x", v) match {
      case Regex(rex, Some("foo"), _) =>
        withClue(s"String [$v] should contain regex [$rex]") {
          v.contains(rex) should be(true)
        }
    }
  }

  it should "accept valid descendant phrase" in {
    val dog = Literal("dog", named = Some("dog"))
    val cat = Literal("cat", named = Some("cat"))
    val mr = SimpleMatcherRegistry(Seq(dog, cat))
    val validDescendantPhrases = Seq("▶$:cat", "▶$d:dog")
    for (v <- validDescendantPhrases) mgp.parseMatcher("v", v, mr) match {
      case w: Wrap =>
      
      case _ => fail(s"failed to parse $v")
    }
  }

  it should "accept valid descendant phrase with predicate" in {
    val dog = Literal("dog", named = Some("dog"))
    val cat = Literal("cat", named = Some("Cat"))
    val mr = SimpleMatcherRegistry(Seq(dog, cat))
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
      
      case _ => fail(s"failed to parse $v")
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
            cat.matchPrefix(InputState(v)) match {
              case Right(PatternMatch(_, matched, InputState(`v`, _, _), _)) =>
              
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
    assert(parsed.name === ".literal")
    parsed match {
      case Literal("xxxx", _) =>
      
      case _ => ???
    }
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

}
