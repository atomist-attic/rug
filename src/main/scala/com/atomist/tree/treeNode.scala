package com.atomist.tree

import com.atomist.rug.spi.{ExportFunction, Typed}
import com.atomist.tree.TreeNode.Significance
import com.atomist.util.{Visitable, Visitor}

/**
  * Represents a node in a tree. May be terminal or non-terminal.
  * This is a core Rug abstraction. Path expressions run against TreeNodes.
  * Rug types are TreeNodes. Some TreeNodes are updatable, some are
  * purely for reference.
  * A TreeNode may be backed by a resource such as
  * an Issue in an issue tracker, by an AST element in a programming language source file,
  * or by a file or directory.
  */
trait TreeNode extends Visitable {

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

  @ExportFunction(readOnly = true, description = "Node content")
  def value: String

  /**
    * Return all visible children of this node
    *
    * @return the children of this node.
    *         Ordering is significant
    */
  def childNodes: Seq[TreeNode] =
    childNodeNames.toSeq.flatMap(name => childrenNamed(name))

  /**
    * Convenience method for Java callers and JavaScript
    */
  @ExportFunction(readOnly = true, description = "Children")
  def children: java.util.List[TreeNode] = {
    import scala.collection.JavaConverters._
    childNodes.asJava
  }

  def childNodeNames: Set[String]

  def childNodeTypes: Set[String]

  override def accept(v: Visitor, depth: Int): Unit = {
    if (v.visit(this, depth))
      childNodes.foreach(_.accept(v, depth + 1))
  }

  def count: Int = childNodes.size

  def childrenNamed(key: String): Seq[TreeNode]

  /**
    * Is this tree node here to help other nodes
    * hang together, or does it have significance
    * to the user and the outside world?
    */
  def significance: Significance = TreeNode.Undeclared

}

object TreeNode {

  sealed trait Significance
  case object Noise extends Significance
  case object Signal extends Significance
  case object Undeclared extends Significance

  /**
    * Tag added to all dynamically created nodes, such as those backed by microgrammars, Antlr or LinkableContainerTreeNodes
    */
  val Dynamic: String = "-dynamic"
}

/**
  * Tag interface for TreeNodes that are intended to contain other TreeNodes.
  */
trait ContainerTreeNode extends TreeNode
