package com.atomist.tree.marshal

import com.atomist.graph.GraphNode

class EmptyContainerGraphNode extends GraphNode {

  override def nodeName: String = "empty"

  override def nodeTags: Set[String] = Set.empty

  override def relatedNodes: Seq[GraphNode] = Nil

  override def relatedNodeNames: Set[String] = Set.empty

  override def relatedNodeTypes: Set[String] = Set.empty

  override def relatedNodesNamed(key: String): Seq[GraphNode] = Nil

}
