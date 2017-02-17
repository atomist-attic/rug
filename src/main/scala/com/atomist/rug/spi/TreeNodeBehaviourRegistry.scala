package com.atomist.rug.spi

import com.atomist.graph.GraphNode

/**
  * Allows Rug to dynamically find all registered Behaviour
  */
trait TreeNodeBehaviourRegistry {

  /**
    * Find a Behaviour by TreeNode and function name
    */
  def findByNodeAndName(treeNode: GraphNode, name: String): Option[TreeNodeBehaviour[GraphNode]]

}
