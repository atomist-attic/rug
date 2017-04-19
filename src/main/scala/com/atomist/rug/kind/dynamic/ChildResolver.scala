package com.atomist.rug.kind.dynamic

import com.atomist.graph.GraphNode
import com.atomist.rug.runtime.js.ExecutionContext
import com.atomist.tree.TreeNode

/**
  * Try to find related nodes of this type from the given context GraphNode.
  * Supports both undirected navigation, via findAllIn, where all related
  * nodes are found, and directed navigation, via navigate, where
  * nodes are found only through a known edge.
  */
trait ChildResolver {

  /**
    * Find all in this context. Return None if
    * it's erroneous to look in this context.
    */
  def findAllIn(context: GraphNode): Option[Seq[TreeNode]]

  /**
    * Directed navigation.
    * Find all in the given context, under the given edge name.
    * Most implementations can return None as per the default.
    * Return None if the edge name is unknown.
    *
    * @param edgeName relationship name
    * @param executionContext context in case we need to resolve repos etc.
    */
  def navigate(context: GraphNode, edgeName: String, executionContext: ExecutionContext): Option[Seq[TreeNode]] =
    None

}
