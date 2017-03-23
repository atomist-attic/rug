package com.atomist.rug.runtime.js

import com.atomist.graph.GraphNode

object SimpleContainerGraphNode {

  def apply(nodeName: String, child: GraphNode, nodeTags: Set[String]): SimpleContainerGraphNode =
    SimpleContainerGraphNode(nodeName, Seq(child), nodeTags)
}

case class SimpleContainerGraphNode(nodeName: String,
                                    relatedNodes: Seq[GraphNode] = Nil,
                                    override val nodeTags: Set[String] = Set.empty)
  extends GraphNode {

  override def relatedNodeNames: Set[String] = relatedNodes.map(_.nodeName).toSet

  override def relatedNodeTypes: Set[String] = relatedNodes.flatMap(_.nodeTags).toSet

  override def relatedNodesNamed(key: String): Seq[GraphNode] =
    relatedNodes.filter(n => n.nodeName == key)

  def addRelatedNode(relatedNode: GraphNode): SimpleContainerGraphNode =
    this.copy(relatedNodes = relatedNodes :+ relatedNode)
}
