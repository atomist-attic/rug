package com.atomist.tree.content.text.microgrammar.matchers

import com.atomist.tree.content.text.microgrammar.{InputState, Literal, PatternMatch}
import org.scalatest.{FlatSpec, Matchers}

class BreakTest extends FlatSpec with Matchers {

  it should "match break in whole string" in {
    val l = Literal("thing") ~ Break(Literal("Y"))
    l.matchPrefix(InputState("thingxxxY")) match {
      case Right(PatternMatch(tn, "thingxxxY", InputState("thingxxxY", _, _), _)) =>
    }
  }

  it should "match break in part of string" in {
    val l = Literal("thing") ~ Break(Literal("Y"))
    l.matchPrefix(InputState("thingxxxYzzz--")) match {
      case Right(pe: PatternMatch) =>
        pe.matched should be ("thingxxxY")
        pe.resultingInputState.input should equal("thingxxxYzzz--")
    }
  }

  it should "not match break with no limit" in {
    val l = Literal("thing") ~ Break(Literal("Y"))
    l.matchPrefix(InputState("thingxxx")) match {
      case Left(_) =>
    }
  }

  it should "match break with long literal string" in {
    val s1 = "the quick brown fox jumped over the lazy dog"
    val s2 = "Y xxx"
    val l = Literal(s1) ~ Break(Literal(s2))
    l.matchPrefix(InputState(s"$s1 and all this nonsense and then $s2 and more garbage")) match {
      case Right(pm) =>
        pm.matched should be (s"$s1 and all this nonsense and then $s2")
    }
  }
}
