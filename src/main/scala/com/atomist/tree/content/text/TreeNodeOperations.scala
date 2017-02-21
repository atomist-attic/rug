package com.atomist.tree.content.text

import com.atomist.tree.TreeNode

/**
  * Operations on TreeNodes
  */
object TreeNodeOperations {

  /**
    * Return all the terminals under the given tree node.
    * Return the node itself if it's terminal
    */
  def terminals(tn: TreeNode): Seq[TreeNode] =
    if (tn.childNodes.isEmpty) Seq(tn)
    else tn.childNodes.flatMap(kid => terminals(kid))

}
