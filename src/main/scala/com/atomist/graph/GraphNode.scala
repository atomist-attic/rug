package com.atomist.graph

import com.atomist.rug.spi.{ExportFunction, Typed}
import com.atomist.tree.utils.NodeUtils
import com.atomist.util.{Visitable, Visitor}

/**
  * Supertrait of all graph nodes. No assumptions about cycles
  * or children.
  *
  * This is a core Rug abstraction. Path expressions run against GraphNodes.
  * A GraphNode is independent of its underlying representation.
  */
trait GraphNode extends Visitable {

  @ExportFunction(readOnly = true, description = "Name of the node")
  def nodeName: String

  @deprecated("Please don't use this", "0.10.0")
  @ExportFunction(readOnly = true, description = "Tags attached to the node")
  def nodeType: Set[String] = nodeTags

  /**
    * Tags for the node, such as "File" or "JavaType". There may be multiple
    * nodes in a tree with the same tags, and multiple tags on the one node.
    * A common use of tags is type.
    *
    * @return tags for the node.
    */
  @ExportFunction(readOnly = true, description = "Tags attached to the node")
  def nodeTags: Set[String] = Set(Typed.typeToTypeName(getClass))

  override def accept(v: Visitor, depth: Int): Unit = {
    if (v.visit(this, depth))
      relatedNodes.foreach(_.accept(v, depth + 1))
  }

  def count: Int = relatedNodes.size

  def relatedNodes: Seq[GraphNode]

  def relatedNodeNames: Set[String]

  def relatedNodeTypes: Set[String]

  def relatedNodesNamed(key: String): Seq[GraphNode]

  /**
    * Find nodes under the named edge, regardless of their names.
    * Default implementation looks on this class for an appropriately
    * named no-arg method returning Seq[GraphNode] or GraphNode
    *
    * @param name name of the edge
    * @return nodes found at the end of this named edge
    */
  def followEdge(name: String): Seq[GraphNode] = {
    NodeUtils.invokeMethodIfPresent[Seq[GraphNode]](this, name) match {
      case Some(l) => l
      case None =>
        NodeUtils.invokeMethodIfPresent[GraphNode](this, name)
          .map(n => Seq(n))
          .getOrElse(Nil)
    }
  }

}
