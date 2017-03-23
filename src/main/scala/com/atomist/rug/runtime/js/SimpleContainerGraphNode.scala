package com.atomist.rug.runtime.js

import com.atomist.graph.GraphNode

object SimpleContainerGraphNode {

  def apply(nodeName: String, child: GraphNode, tags: String*): SimpleContainerGraphNode =
    SimpleContainerGraphNode(nodeName, Seq(child), tags.toSet)

  def empty(nodeName: String, tags: String*): SimpleContainerGraphNode =
    SimpleContainerGraphNode(nodeName, Nil, tags.toSet)
}

case class SimpleContainerGraphNode(nodeName: String,
                                    relatedNodes: Seq[GraphNode],
                                    override val nodeTags: Set[String])
  extends GraphNode {

  override def relatedNodeNames: Set[String] = relatedNodes.map(_.nodeName).toSet

  override def relatedNodeTypes: Set[String] = relatedNodes.flatMap(_.nodeTags).toSet

  override def relatedNodesNamed(key: String): Seq[GraphNode] =
    relatedNodes.filter(n => n.nodeName == key)

  def addRelatedNode(relatedNode: GraphNode): SimpleContainerGraphNode =
    this.copy(relatedNodes = relatedNodes :+ relatedNode)

  def withTag(tag: String): SimpleContainerGraphNode = copy(nodeTags = nodeTags ++ Set(tag))
}
