package com.atomist.tree.content.text.microgrammar

import org.scalatest.{FlatSpec, Matchers}

class RegexTest extends FlatSpec with Matchers {

  it should "match greedy regex" in {
    val name = "foobar"
    val l = Regex("[a-zA-Z]+", Some(name))
    l.matchPrefix(InputState("thingxxxY")) match {
      case Right(PatternMatch(tn, "thingxxxY", InputState2("thingxxxY", _, _), _)) =>
        tn.nodeName should be (name)
      case _ => ???
    }
  }

  it should "match greedy regex followed by other content" in {
    val name = "foobar"
    val l = Regex("[a-zA-Z]+", Some(name))
    l.matchPrefix(InputState("thingxxxY0")) match {
      case Right(PatternMatch(tn, "thingxxxY", InputState2("thingxxxY0", _, _), _)) =>
        tn.nodeName should be (name)
      case _ => ???
    }
  }
}
