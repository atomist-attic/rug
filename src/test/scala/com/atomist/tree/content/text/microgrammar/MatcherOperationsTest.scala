package com.atomist.tree.content.text.microgrammar

import org.scalatest.{FlatSpec, Matchers}

class MatcherOperationsTest extends FlatSpec with Matchers {

  it should "match literal in whole string" in {
    val l = Literal("thing", named = Some("x"))
    val ls = l.toString
    l.matchPrefix(0, "thing") match {
      case Some(PatternMatch(tn, 0, "thing", "thing", `ls`)) =>
        tn.get.nodeName should be ("x")
        tn.get.value should be ("thing")
    }
  }

  it should "match literal in partial string" in {
    val l = Literal("thing")
    l.matchPrefix(0, "thing2") should equal(Some(PatternMatch(None, 0, "thing", "thing2", l.toString)))
  }

  it should "concatenate literals" in {
    val l1 = Literal("thing")
    val l2 = Literal("2")
    val l = l1 ~ l2
    l.matchPrefix(0, "thing2") match {
      case Some(PatternMatch(tn, 0, "thing2", "thing2", _)) =>
    }
  }

  it should "concatenate literals that don't match" in {
    val l1 = Literal("thing")
    val l2 = Literal("22222")
    val l = l1 ~ l2
    l.matchPrefix(0, "thing2") match {
      case None =>
    }
  }

  it should "match alternate" in {
    val l1 = Literal("thing")
    val l2 = Literal("2")
    val l = l1 | l2
    l.matchPrefix(0, "thing") match {
      case Some(PatternMatch(tn, 0, "thing", "thing", _)) =>
    }
    l.matchPrefix(0, "2") match {
      case Some(PatternMatch(tn, 0, "2", "2", _)) =>
    }
    l.matchPrefix(0, "thing2") match {
      case Some(PatternMatch(tn, 0, "thing", "thing2", _)) =>
    }
  }

  it should "match opt" in {
    val l1 = Literal("thing")
    val l2 = Literal("2")
    val l = l1.? ~ l2
    l.matchPrefix(0, "thing2") match {
      case Some(PatternMatch(tn, 0, "thing2", "thing2", _)) =>
    }
    l.matchPrefix(0, "2") match {
      case Some(PatternMatch(tn, 0, "2", "2", _)) =>
    }
    l.matchPrefix(0, "thing2") match {
      case Some(PatternMatch(tn, 0, "thing2", "thing2", _)) =>
    }
    l.matchPrefix(0, "xthing2") match {
      case None =>
    }
  }
}