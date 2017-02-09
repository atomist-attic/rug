package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.ContainerTreeNode
import com.atomist.tree.content.text.PositionedMutableContainerTreeNode
import com.atomist.tree.utils.TreeNodeUtils
import org.scalatest.{FlatSpec, Matchers}

class WrapTest extends FlatSpec with Matchers {

  it should "handle wrap of regex" in {
    val l1 = Regex("thing", Some("t...g"))
    val l = Wrap(l1, "higherLevel")
    val input = "thingthing2"
    l.matchPrefix(InputState(input)) match {
      case Right(pe: PatternMatch) =>
        assert(pe.matched === "thing")
        assert(pe.resultingInputState.input === input)
        assert(pe.resultingInputState.offset === "thing".length)
        val tn = pe.node.asInstanceOf[ContainerTreeNode]
        assert(tn.nodeName === "higherLevel")
        assert(tn.childNodes.size === 1)
      case _ => ???
    }
  }

  it should "handle rep of regex with two instances" in {
    val l1 = Regex("t...g", Some("thing"))
    val rl = Rep(l1, None)
    val l = Wrap(rl, "higherLevel")
    val input = "thingthing2"

    l.matchPrefix(InputState(input)) match {
      case Right(pe: PatternMatch) =>
        assert(pe.matched === "thingthing")
        assert(pe.resultingInputState.input === input)
        assert(pe.resultingInputState.offset === "thingthing".length)
        val tn = pe.node.asInstanceOf[PositionedMutableContainerTreeNode]
        assert(tn.nodeName === l.name)

        tn.pad(input)

        //println("After pad: " + TreeNodeUtils.toShortString(tn))
        //println(tn.childNodes)
        assert(tn.childNodes.size === 2)
       // tn.childNodes.head.nodeName should be (rl.name)
      case Left(dr) =>
        //println(DismatchReport.detailedReport(dr, input))
        fail
    }
  }

}
