package com.atomist.tree.content.text

import com.atomist.tree.TreeNode
import org.scalatest.{FlatSpec, Matchers}

class TrivialTreeNode(name: String, var _children: Seq[TreeNode] = Seq()) extends TreeNode {

  override def childNodes = _children

  override def nodeName: String = name

  override def relatedNodes: Seq[TreeNode] = ???

  override def relatedNodeNames: Set[String] = ???

  override def relatedNodeTypes: Set[String] = ???

  override def relatedNodesNamed(key: String): Seq[TreeNode] = ???

  override def value: String = ???

  override def childrenNamed(key: String): Seq[TreeNode] = ???

  override def childNodeNames: Set[String] = ???

  override def childNodeTypes: Set[String] = ???
}

class TreeNodeOperationsTest extends FlatSpec with Matchers {

  import TreeNodeOperations._

  it should "find terminals under a terminal" in {
    val tn = new TrivialTreeNode("x")
    terminals(tn) should equal(Seq(tn))
  }

  it should "find terminals under a container" in {
    val tn1 = new TrivialTreeNode("x")
    val tn2 = new TrivialTreeNode("x2")
    val mc = new TrivialTreeNode("foo", Seq(tn1, tn2))
    terminals(mc) should equal(Seq(tn1, tn2))
  }

}
