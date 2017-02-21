package com.atomist.tree.content.text

import com.atomist.tree.{ParentAwareTreeNode, TreeNode}

/**
  * Operations on TreeNodes
  */
object TreeNodeOperations {

  /**
    * Is a given node a known ancestor
    */
  def isKnownAncestor(n: TreeNode, possibleParent: TreeNode): Boolean =
    knownAncestorMatching(n, tn => possibleParent == tn).isDefined

  def knownAncestorMatching(n: TreeNode, test: TreeNode => Boolean): Option[TreeNode] = n match {
    case patn: ParentAwareTreeNode if test(patn.parent) => Some(patn)
    case patn: ParentAwareTreeNode if patn.parent == null => None
    case patn: ParentAwareTreeNode => knownAncestorMatching(patn.parent, test)
    case _ => None
  }

  /**
    * Return all the terminals under the given tree node.
    * Return the node itself if it's terminal
    */
  def terminals(tn: TreeNode): Seq[TreeNode] =
    if (tn.childNodes.isEmpty) Seq(tn)
    else tn.childNodes.flatMap(kid => terminals(kid))

}
