package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.TreeNode
import org.scalatest.{FlatSpec, Matchers}

class MatcherOperationsTest extends FlatSpec with Matchers {

  it should "match literal in whole string" in {
    val l = Literal("thing", named = Some("x"))
    val ls = l.toString
    l.matchPrefix(InputState("thing")) match {
      case Right(PatternMatch(tn, "thing", InputState("thing", _, _), `ls`)) =>
        assert(tn.nodeName === "x")
        assert(tn.value === "thing")
      case _ => ???
    }
  }

  it should "match literal in partial string" in {
    val l = Literal("thing")
    l.matchPrefix(InputState("thing2")) match {
      case Right(PatternMatch(node, "thing", is, value)) =>
        assert(node.significance === TreeNode.Noise)
        value should be(l.toString)
        assert(is.offset === 5)
      case _ => ???
    }
  }

  it should "concatenate literals" in {
    val l1 = Literal("thing")
    val l2 = Literal("2")
    val l = l1 ~ l2
    l.matchPrefix(InputState("thing2")) match {
      case Right(PatternMatch(tn, "thing2", InputState(thing2, _, _), _)) =>
      
      case _ => ???
    }
  }

  it should "concatenate literals that don't match" in {
    val l1 = Literal("thing")
    val l2 = Literal("22222")
    val l = l1 ~ l2
    l.matchPrefix(InputState("thing2")) match {
      case Left(_) =>
      
      case _ => ???
    }
  }

  it should "match alternate" in {
    val l1 = Literal("thing")
    val l2 = Literal("2")
    val l = l1 | l2
    l.matchPrefix(InputState("thing")) match {
      case Right(PatternMatch(_, "thing", InputState("thing", _, _), _)) =>
      
      case _ => ???
    }
    l2.matchPrefix(InputState("2")) match {
      case Right(PatternMatch(_, "2", InputState("2", _, 1), _)) =>
      
      case _ => ???
    }
    l.matchPrefix(InputState("2")) match {
      case Right(PatternMatch(_, "2", InputState("2", _, 1), _)) =>
      
      case _ => ???
    }
    l.matchPrefix(InputState("thing2")) match {
      case Right(PatternMatch(tn, "thing", InputState("thing2", _, _), _)) =>
      
      case _ => ???
    }
  }

  it should "match opt" in {
    val l1 = Literal("thing")
    val l = l1.?
    l.matchPrefix(InputState("thing2")) match {
      case Right(PatternMatch(_, "thing", InputState("thing2", _, 5), _)) =>
      
      case _ => ???
    }
  }

  it should "match opt in alternate" in {
    val l1 = Literal("thing")
    val l2 = Literal("2")
    val l = l1.? ~ l2
    l.matchPrefix(InputState("thing2")) match {
      case Right(PatternMatch(tn, "thing2", InputState("thing2", _, _), _)) =>
      
      case _ => ???
    }
    l.matchPrefix(InputState("2")) match {
      case Right(PatternMatch(tn, "2", InputState("2", _, _), _)) =>
      
      case _ => ???
    }
    l.matchPrefix(InputState("thing2")) match {
      case Right(PatternMatch(tn, "thing2", InputState("thing2", _, _), _)) =>
      
      case _ => ???
    }
    l.matchPrefix(InputState("xthing2")) match {
      case Left(_) =>
      
      case _ => ???
    }
  }
}