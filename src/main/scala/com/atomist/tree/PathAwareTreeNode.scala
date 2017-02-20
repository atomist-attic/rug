package com.atomist.tree

/**
  * Node that knows its address in hierarchy
  */
trait AddressableTreeNode extends TreeNode {

  def address: String
}

/**
  * Tree node aware of its parentage and path
  */
trait PathAwareTreeNode extends TreeNode with AddressableTreeNode {

  /**
    * Nullable if at the top level, as we don't want to complicate
    * use from JavaScript by using Option.
    */
  def parent: PathAwareTreeNode

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