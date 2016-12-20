package com.atomist.tree

import com.atomist.rug.spi.Typed
import com.atomist.util.{Visitable, Visitor}

/**
  * Represents a node in a tree. May be terminal or non-terminal.
  */
trait TreeNode extends Visitable {

  /**
    * Name of the node. This may vary with individual nodes: For example,
    * with files. However, node names do not always need to be unique.
    * @return name of the individual node
    */
  // TODO maybe names could be unique now
  def nodeName: String

  /**
    * Type of the node, such as "File" or "JavaType". There may be multiple
    * nodes in a tree with the same type.
    * @return the type of the node.
    */
  def nodeType: String = Typed.typeToTypeName(getClass)

  /**
    * All nodes have values: Either a terminal value or the
    * values built up from subnodes.
    */
  def value: String

}

/**
  * TreeNode that can contain other TreeNodes.
  */
trait ContainerTreeNode extends TreeNode {

  /**
    * Return all children of this node
    * @return all the children of this node.
    *         Ordering is significant
    */
  def childNodes: Seq[TreeNode]

  /**
    * Return the names of children of this node
    * @return the names of children. There may be multiple children
    *         with a given name
    */
  def childNodeNames: Set[String] =
    if (childNodes != null)
      childNodes.map(_.nodeName).toSet
    else Set()

  def childNodeTypes: Set[String]

  override def accept(v: Visitor, depth: Int): Unit = {
    if (v.visit(this, depth))
      childNodes.foreach(_.accept(v, depth + 1))
  }

  def dirty: Boolean =
    (childNodes collect {
      case u: MutableTreeNode if u.dirty => u
    }).nonEmpty

  def count: Int = childNodes.size

  /**
    * Children under the given key. May be empty.
    *
    * @param key field name
    */
  def apply(key: String): Seq[TreeNode] =
    childNodes.filter(f => f.nodeName.equals(key))
}

/**
  * Terminal TreeNode. Contains a simple string value.
  */
trait TerminalTreeNode extends TreeNode {

  override def accept(v: Visitor, depth: Int): Unit = v.visit(this, depth)
}

/**
  * Convenient terminal node implementation.
  * Prefer PaddingNode for padding.
  * @param nodeName name of the field (usually unimportant)
  * @param value field content.
  */
case class SimpleTerminalTreeNode(nodeName: String, value: String, override val nodeType: String = "literal")
  extends TerminalTreeNode

/**
  * Convenient class for padding nodes
  * @param description description of what's being padded
  * @param value padding content
  */
case class PaddingNode(description: String, value: String) extends TerminalTreeNode {

  override def nodeName = s"padding:$description"

  override def nodeType = "padding"
}
