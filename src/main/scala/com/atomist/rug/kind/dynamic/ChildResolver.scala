package com.atomist.rug.kind.dynamic

import com.atomist.tree.TreeNode

/**
  * Try to find children of this type in the given context
  */
trait ChildResolver {

  /**
    * Find all in this context
    *
    * @param context
    */
  def findAllIn(context: TreeNode): Option[Seq[TreeNode]]
}
