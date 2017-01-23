package com.atomist.tree.content.text.microgrammar.matchers

import com.atomist.tree.content.text.microgrammar.{InputState, Literal, PatternMatch}
import org.scalatest.{FlatSpec, Matchers}

class BreakTest extends FlatSpec with Matchers {

  it should "match break in whole string" in {
    val l = Literal("thing") ~ Break(Literal("Y"))
    l.matchPrefix(InputState("thingxxxY")) match {
      case Some(PatternMatch(tn, "thingxxxY", InputState("thingxxxY", _, _), _)) =>
    }
  }

  it should "match break in part of string" in {
    val l = Literal("thing") ~ Break(Literal("Y"))
    l.matchPrefix(InputState("thingxxxYzzz")) match {
      case Some(pe: PatternMatch) =>
        pe.matched should be ("thingxxxY")
        pe.resultingInputState.input should equal("thingxxxYzzz")
    }
  }

  it should "not match break with no limit" in {
    val l = Literal("thing") ~ Break(Literal("Y"))
    l.matchPrefix(InputState("thingxxx")) match {
      case None =>
    }
  }
}
