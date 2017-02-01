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
        pe.matched should be ("thing")
        pe.resultingInputState.input should be (input)
        pe.resultingInputState.offset should be ("thing".length)
        val tn = pe.node.get.asInstanceOf[ContainerTreeNode]
        tn.nodeName should be ("higherLevel")
        tn.childNodes.size should be (1)
      case _ => ???
    }
  }

  it should "handle rep of regex with two instances" in {
    val l1 = Regex("t...g", Some("thing"))
    val rl = Rep(l1, None)//"reppyreprep")
    val l = Wrap(rl, "higherLevel")
    val input = "thingthing2"

    l.matchPrefix(InputState(input)) match {
      case Right(pe: PatternMatch) =>
        pe.matched should be ("thingthing")
        pe.resultingInputState.input should be (input)
        pe.resultingInputState.offset should be ("thingthing".length)
        val tn = pe.node.get.asInstanceOf[PositionedMutableContainerTreeNode]
        tn.nodeName should be (l.name)
//        println("Before pad: " + TreeNodeUtils.toShortString(tn))
//        println(tn.childNodes)

        tn.pad(input)

        //println("After pad: " + TreeNodeUtils.toShortString(tn))

        println(tn.childNodes)
        tn.childNodes.size should be (2)
       // tn.childNodes.head.nodeName should be (rl.name)
      case Left(dr) =>
        println(DismatchReport.detailedReport(dr, input))
        fail
    }
  }

}
