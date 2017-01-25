package com.atomist.tree.content.text.microgrammar

import org.scalatest.{FlatSpec, Matchers}

class MatcherOperationsTest extends FlatSpec with Matchers {

  it should "match literal in whole string" in {
    val l = Literal("thing", named = Some("x"))
    val ls = l.toString
    l.matchPrefix(InputState("thing")) match {
      case Right(PatternMatch(tn, "thing", InputState("thing", _, _), `ls`)) =>
        tn.get.nodeName should be ("x")
        tn.get.value should be ("thing")
    }
  }

  it should "match literal in partial string" in {
    val l = Literal("thing")
    l.matchPrefix(InputState("thing2")) match {
      case Right(pm) =>
        pm should equal(
          PatternMatch(None, "thing", InputState("thing2", offset = 5), l.toString))
    }
  }

  it should "concatenate literals" in {
    val l1 = Literal("thing")
    val l2 = Literal("2")
    val l = l1 ~ l2
    l.matchPrefix(InputState("thing2")) match {
      case Right(PatternMatch(tn, "thing2", InputState(thing2, _, _), _)) =>
    }
  }

  it should "concatenate literals that don't match" in {
    val l1 = Literal("thing")
    val l2 = Literal("22222")
    val l = l1 ~ l2
    l.matchPrefix(InputState("thing2")) match {
      case Left(_) =>
    }
  }

  it should "match alternate" in {
    val l1 = Literal("thing")
    val l2 = Literal("2")
    val l = l1 | l2
    l.matchPrefix(InputState("thing")) match {
      case Right(PatternMatch(_, "thing", InputState("thing", _, _), _)) =>
    }
    l2.matchPrefix(InputState("2")) match {
      case Right(PatternMatch(_, "2", InputState("2", _, 1), _)) =>
    }
    l.matchPrefix(InputState("2")) match {
      case Right(PatternMatch(_, "2", InputState("2", _, 1), _)) =>
    }
    l.matchPrefix(InputState("thing2")) match {
      case Right(PatternMatch(tn, "thing", InputState("thing2", _, _), _)) =>
    }
  }

  it should "match opt" in {
    val l1 = Literal("thing")
    val l = l1.?
    l.matchPrefix(InputState("thing2")) match {
      case Right(PatternMatch(_, "thing", InputState("thing2", _, 5), _)) =>
    }
  }

  it should "match opt in alternate" in {
    val l1 = Literal("thing")
    val l2 = Literal("2")
    val l = l1.? ~ l2
    l.matchPrefix(InputState("thing2")) match {
      case Right(PatternMatch(tn, "thing2", InputState("thing2", _, _), _)) =>
    }
    l.matchPrefix(InputState("2")) match {
      case Right(PatternMatch(tn, "2", InputState("2", _, _), _)) =>
    }
    l.matchPrefix(InputState("thing2")) match {
      case Right(PatternMatch(tn, "thing2", InputState("thing2", _, _), _)) =>
    }
    l.matchPrefix(InputState("xthing2")) match {
      case Left(_) =>
    }
  }
}