package com.atomist.tree.content.text.microgrammar

import org.scalatest.{FlatSpec, Matchers}

class RegexTest extends FlatSpec with Matchers {

  it should "match greedy regex" in {
    val name = "foobar"
    val l = Regex(name, "[a-zA-Z]+")
    l.matchPrefix(InputState("thingxxxY")) match {
      case Some(PatternMatch(tn, "thingxxxY", InputState("thingxxxY", _, _), _)) =>
        tn.get.nodeName should be (name)
    }
  }

  it should "match greedy regex followed by other content" in {
    val name = "foobar"
    val l = Regex(name, "[a-zA-Z]+")
    l.matchPrefix(InputState("thingxxxY0")) match {
      case Some(PatternMatch(tn, "thingxxxY", InputState("thingxxxY0", _, _), _)) =>
        tn.get.nodeName should be (name)
    }
  }
}
