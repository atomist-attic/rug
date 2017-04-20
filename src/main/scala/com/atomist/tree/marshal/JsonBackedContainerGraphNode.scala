package com.atomist.tree.marshal

import com.atomist.graph.GraphNode

/**
  * Allows us to return a ContainerTreeNode that encapsulates a json representation
  * of the object tree.
  *
  * @param innerNode The ContainerTreeNode that we are encapsulating
  * @param backingJson the json string that also represents this object tree
  */
class JsonBackedContainerGraphNode(val innerNode: GraphNode,
                                   val backingJson: String,
                                   val version: String)
  extends GraphNode {

  override def nodeName: String = innerNode.nodeName

  override def nodeTags: Set[String] = innerNode.nodeTags

  override def relatedNodes: Seq[GraphNode] = innerNode.relatedNodes

  override def relatedNodeNames: Set[String] = innerNode.relatedNodeNames

  override def relatedNodeTypes: Set[String] = innerNode.relatedNodeTypes

  override def relatedNodesNamed(key: String): Seq[GraphNode] = innerNode.relatedNodesNamed(key)

  def jsonRepresentation: String = backingJson
}
