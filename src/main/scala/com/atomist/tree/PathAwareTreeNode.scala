package com.atomist.tree

import com.atomist.graph.AddressableGraphNode

trait AddressableTreeNode extends AddressableGraphNode with TreeNode

trait ParentAwareTreeNode extends TreeNode {

  /**
    * Nullable if at the top level, as we don't want to complicate
    * use from JavaScript by using Option.
    */
  def parent: TreeNode
}

/**
  * Tree node aware of its parentage and path
  */
trait PathAwareTreeNode extends ParentAwareTreeNode with AddressableTreeNode {

  /**
    * Override to make type more specific. Null if we're top level.
    */
  override def parent: PathAwareTreeNode

  override def address: String = PathAwareTreeNode.address(this, s"[@name='$nodeName']")
}

object PathAwareTreeNode {

  def address(nodeOfInterest: PathAwareTreeNode, test: String): String = {
    val myType = nodeOfInterest.nodeTags.mkString(",")
    if (nodeOfInterest.parent == null)
      s"$myType()$test"
    else
      s"${nodeOfInterest.parent.address}/$myType()[$test]"
  }
}