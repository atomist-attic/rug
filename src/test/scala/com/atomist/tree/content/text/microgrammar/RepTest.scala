package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.ContainerTreeNode
import org.scalatest.{FlatSpec, Matchers}

class RepTest extends FlatSpec with Matchers {

  it should "handle rep of literal with 0 matches" in {
    val l1 = Literal("thing")
    val l = Rep(l1)
    val input = "woieurowieurowieur"
    l.matchPrefix(InputState(input)) match {
      case Right(PatternMatch(tn, "", InputState(input, _, _), _)) =>
        assert(tn.nodeName === ".rep")
      case _ => ???
    }
  }

  it should "handle rep of literal with single instance" in {
    val l1 = Literal("thing", Some("thing"))
    val l = Rep(l1)
    l.matchPrefix(InputState("thing2")) match {
      case Right(PatternMatch(tn: ContainerTreeNode, "thing", InputState(thing2, _, _), _)) =>
        assert(tn.nodeName === ".rep")
        assert(tn.childNodes.size === 1)
      case _ => ???
    }
  }

  it should "handle rep of literal with two instances" in {
    val l1 = Literal("thing", Some("thing"))
    val l = Rep(l1)
    val input = "thingthing2"
    l.matchPrefix(InputState(input)) match {
      case Right(pe: PatternMatch) =>
        assert(pe.matched === "thingthing")
        assert(pe.resultingInputState.input === input)
        assert(pe.resultingInputState.offset === "thingthing".length)
        val tn = pe.node.asInstanceOf[ContainerTreeNode]
        assert(tn.nodeName === ".rep")
        assert(tn.childNodes.size === 2)
      case _ => ???
    }
  }

  it should "handle rep of regex with two instances" in {
    val l1 = new Regex("thing", Some("t...g"))
    val namedRep = Rep(l1, None)//"myReppyName")
    val input = "thingthing2"
    namedRep.matchPrefix(InputState(input)) match {
      case Right(pe: PatternMatch) =>
        assert(pe.matched === "thingthing")
        assert(pe.resultingInputState.input === input)
        assert(pe.resultingInputState.offset === "thingthing".length)
        val tn = pe.node.asInstanceOf[ContainerTreeNode]
        assert(tn.nodeName === namedRep.name)
        assert(tn.childNodes.size === 2)
      case _ => ???
    }
  }
}



