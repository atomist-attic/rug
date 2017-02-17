package com.atomist.rug.spi

import java.util.Set

import com.atomist.tree.TreeNode

/**
  * Attaches behaviour to the a TreeNode.
  */
trait TreeNodeBehaviour[T <: TreeNode] {

  /**
    * The TreeNode the name function should become available
    */
  def nodeTypes: Set[String]

  /**
    * Name of the function on TreeNode
    */
  def name: String

  /**
    * Invoke the function on the given TreeNode
    */
  def invokeOn(treeNode: T): Object
}