package com.atomist.tree.content.text.microgrammar

import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by rod on 10/29/16.
  */
class RegexTest extends FlatSpec with Matchers {

  it should "match greedy regex" in {
    val name = "foobar"
    val l = Regex(name, "[a-zA-Z]+")
    l.matchPrefix(0, "thingxxxY") match {
      case Some(PatternMatch(tn, 0, "thingxxxY", "thingxxxY", _)) =>
        tn.get.nodeName should be (name)
    }
  }

  it should "match greedy regex followed by other content" in {
    val name = "foobar"
    val l = Regex(name, "[a-zA-Z]+")
    l.matchPrefix(0, "thingxxxY0") match {
      case Some(PatternMatch(tn, 0, "thingxxxY", "thingxxxY0", _)) =>
        tn.get.nodeName should be (name)
    }
  }
}
