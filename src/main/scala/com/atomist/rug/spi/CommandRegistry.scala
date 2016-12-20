package com.atomist.rug.spi

import com.atomist.tree.TreeNode

/**
  * Allows Rug to dynamically find all registered Commands
  */
trait CommandRegistry {

  /**
    * Find a Command by TreeNode and function name
    */
  def findByNodeAndName(treeNode: TreeNode, name: String): Option[Command[TreeNode]]

}
