package com.atomist.graph

import com.atomist.rug.spi.{ExportFunction, Typed}
import com.atomist.util.{Visitable, Visitor}

/**
  * Common to all grah and tree nodes
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

}
