package com.atomist.model.content.grammar.microgrammar.pattern

import org.scalatest.{FlatSpec, Matchers}

class BreakTest extends FlatSpec with Matchers {

  it should "match break in whole string" in {
    val l = Literal("thing") ~ Break(Literal("Y"))
    l.matchPrefix(0, "thingxxxY") match {
      case Some(PatternMatch(tn, 0, "thingxxx", "thingxxxY", _)) =>
    }
  }

  it should "match break in part of string" in {
    val l = Literal("thing") ~ Break(Literal("Y"))
    l.matchPrefix(0, "thingxxxYzzz") match {
      case Some(PatternMatch(tn, 0, "thingxxx", "thingxxxYzzz", _)) =>
    }
  }

  it should "not match break with no limit" in {
    val l = Literal("thing") ~ Break(Literal("Y"))
    l.matchPrefix(0, "thingxxx") match {
      case None =>
    }
  }
}

