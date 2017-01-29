package com.atomist.tree.content.text.microgrammar.matchers

import com.atomist.tree.content.text.microgrammar.{InputState, InputState2, Literal, PatternMatch}
import org.scalatest.{FlatSpec, Matchers}

class BreakTest extends FlatSpec with Matchers {

  it should "match break in whole string" in {
    val l = Literal("thing") ~ Break(Literal("Y"))
    l.matchPrefix(InputState("thingxxxY")) match {
      case Right(PatternMatch(tn, "thingxxxY", InputState2("thingxxxY", _, _), _)) =>
      case _ => ???
    }
  }

  it should "match break in part of string" in {
    val l = Literal("thing") ~ Break(Literal("Y"))
    l.matchPrefix(InputState("thingxxxYzzz--")) match {
      case Right(pe: PatternMatch) =>
        assert(pe.matched === "thingxxxY")
        assert(pe.resultingInputState.input === "thingxxxYzzz--")
      case _ => ???
    }
  }

  it should "not match break with no limit" in {
    val l = Literal("thing") ~ Break(Literal("Y"))
    l.matchPrefix(InputState("thingxxx")) match {
      case Left(_) =>
      
      case _ => ???
    }
  }

  it should "match break with long literal string" in {
    val s1 = "the quick brown fox jumped over the lazy dog"
    val s2 = "Y xxx"
    val l = Literal(s1) ~ Break(Literal(s2))
    l.matchPrefix(InputState(s"$s1 and all this nonsense and then $s2 and more garbage")) match {
      case Right(pm) =>
        assert(pm.matched === s"$s1 and all this nonsense and then $s2")
      case _ => ???
    }
  }
}
