package com.atomist.tree.marshal

import com.atomist.graph.GraphNode
import com.atomist.rug.spi.ExportFunction
import com.atomist.tree.pathexpression.PathExpression

/**
  * A node has been encountered in the path expression that cannot be resolved by the materializing
  * implementation. A suitable interface by be able to continue to resolve further. This class
  * is the parent class of such nodes and usually should be extended.
  *
  * As an example a NeoTreeMaterializer is able to resolve across the Neo database but when it finds
  * a repo with folders is unable to proceed further down that path. It will return an UnresolvableNode with the
  * remaining path expression as a property.
  *
  * @param remainingPathExpression The parts of the path expression that were unable to be resolved
  */
class UnresolvableNode(val remainingPathExpression: PathExpression)
  extends GraphNode {

  override def relatedNodes: Seq[GraphNode] = ???

  override def relatedNodeNames: Set[String] = ???

  override def relatedNodeTypes: Set[String] = ???

  override def relatedNodesNamed(key: String): Seq[GraphNode] = ???

  @ExportFunction(readOnly = true, description = "Name of the node")
  override def nodeName: String = "UnresolveableNode"
}
