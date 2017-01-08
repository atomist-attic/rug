package com.atomist.tree

import com.atomist.rug.spi.{ExportFunction, Typed}
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

  /**
    * Can this node be obtained from a top level type such as File?
    */
  val searchable: Boolean = true

  /**
    * Name of the node. This may vary with individual nodes: For example,
    * with files. However, node names do not always need to be unique.
    * @return name of the individual node
    */
  @ExportFunction(readOnly = true, description = "Name of the node")
  def nodeName: String

  /**
    * Type of the node, such as "File" or "JavaType". There may be multiple
    * nodes in a tree with the same type.
    * @return the type of the node.
    */
  @ExportFunction(readOnly = true, description = "Type of the node")
  def nodeType: Set[String] = Set(Typed.typeToTypeName(getClass))

  /**
    * All nodes have values: Either a terminal value or the
    * values built up from subnodes.
    */
  @ExportFunction(readOnly = true, description = "Node content")
  def value: String

}

/**
  * TreeNode that can contain other TreeNodes.
  */
trait ContainerTreeNode extends TreeNode {

  /**
    * Return all children of this node
    *
    * @return all the children of this node.
    *         Ordering is significant
    */
  def childNodes: Seq[TreeNode] =
    childNodeNames.toSeq.flatMap(name => childrenNamed(name))

  /**
    * Return the names of children of this node
    *
    * @return the names of children. There may be multiple children
    *         with a given name
    */
  def childNodeNames: Set[String]

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
  def childrenNamed(key: String): Seq[TreeNode]

}

/**
  * Terminal TreeNode. Contains a simple string value.
  */
trait TerminalTreeNode extends TreeNode {

  override def accept(v: Visitor, depth: Int): Unit = v.visit(this, depth)
}

