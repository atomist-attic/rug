package com.atomist.rug.spi

import com.atomist.tree.TreeNode

/**
  * Simple command that attaches behaviour to the a TreeNode.
  */
trait Command[T <: TreeNode] {

  /**
    * The TreeNode the name function should become available
    */
  def `type`: String

  /**
    * Name of the function on TreeNode
    */
  def name: String

  /**
    * Invoke the command on the given TreeNode
    */
  def invokeOn(treeNode: T): Object
}