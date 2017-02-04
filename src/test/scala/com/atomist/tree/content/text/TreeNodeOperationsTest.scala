package com.atomist.tree.content.text

import com.atomist.tree.SimpleTerminalTreeNode
import org.scalatest.{FlatSpec, Matchers}

class TreeNodeOperationsTest extends FlatSpec with Matchers {

  import TreeNodeOperations._

  it should "find terminals under a terminal" in {
    val tn = SimpleTerminalTreeNode("x", "y")
    terminals(tn) should equal (Seq(tn))
  }

  it should "find terminals under a container" in {
    val tn1 =  SimpleTerminalTreeNode("x", "y")
    val tn2 =  SimpleTerminalTreeNode("x2", "y")
    val mc = new ParsedMutableContainerTreeNode("foo")
    mc.appendField(tn1)
    mc.appendField(tn2)
    terminals(mc) should equal (Seq(tn1, tn2))
  }

}
