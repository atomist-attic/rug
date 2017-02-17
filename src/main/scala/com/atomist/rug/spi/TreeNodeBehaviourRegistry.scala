package com.atomist.rug.spi

import com.atomist.tree.TreeNode

/**
  * Allows Rug to dynamically find all registered Behaviour
  */
trait TreeNodeBehaviourRegistry {

  /**
    * Find a Behaviour by TreeNode and function name
    */
  def findByNodeAndName(treeNode: TreeNode, name: String): Option[TreeNodeBehaviour[TreeNode]]

}
