package com.atomist.tree.content.text.microgrammar.dsl

import com.atomist.parse.java.ParsingTargets
import com.atomist.tree.content.text.microgrammar.MatcherMicrogrammar
import org.scalatest.{FlatSpec, Matchers}

class MatcherUsageTest extends FlatSpec with Matchers {

  val mgp = new MatcherDefinitionParser

  it should "match literal" in {
    val matcher = mgp.parse("def foo")
    matcher.matchPrefix(0, "def foo thing") match {
      case Some(pm) =>
    }
  }

  // TODO this is a debatable case. Why wouldn't we just match with a regex or literal string
  // if there's nothing dynamic in the content? No nodes are created
  it should "match literal using microgrammar" in pendingUntilFixed {
    val matcher = mgp.parse("def foo")
    //println(matcher)
    // Problem is that this is discarded as nothing is bound
    val mg = new MatcherMicrogrammar("deff", matcher)
    mg.findMatches("def foo bar").size should be(1)
  }

  it should "match regex using microgrammar" in {
    val matcher = mgp.parse("def $foo:§f.o§")
    //println(matcher)
    val mg = new MatcherMicrogrammar("deff", matcher)
    val input = "def foo bar"
    matcher.matchPrefix(0, input) match {
      case Some(pm) =>
    }
    mg.findMatches(input).size should be(1)
  }

  it should "match regex in Maven POM" in {
    val proj = ParsingTargets.NewStartSpringIoProject

    val mg = new MatcherMicrogrammar("gid",
      mgp.parse("<modelVersion>$modelVersion:§[a-zA-Z0-9_\\.]+§</modelVersion>"))
    println(mg.matcher)
    val pom = proj.findFile("pom.xml").get.content
    val matches = mg.findMatches(pom)
    println(matches)
    matches.size should be(1)
  }

}
