package com.atomist.tree

import com.atomist.graph.GraphNode
import com.atomist.rug.spi.ExportFunction
import com.atomist.tree.TreeNode.Significance

/**
  * Represents a node in a tree. A tree is a graph that is
  * hierarchical but non-cyclical.
  * May be terminal or non-terminal.
  * This is a core Rug abstraction. Path expressions run against TreeNodes.
  * Rug types are TreeNodes. Some TreeNodes are updatable, some are
  * purely for reference.
  * A TreeNode may be backed by a resource such as
  * an Issue in an issue tracker, by an AST element in a programming language source file,
  * or by a file or directory.
  */
trait TreeNode extends GraphNode {

  @ExportFunction(readOnly = true, description = "Node content")
  def value: String

  def relatedNodes: Seq[GraphNode] = childNodes

  def relatedNodesNamed(key: String): Seq[GraphNode] = childrenNamed(key)

  def childrenNamed(key: String): Seq[TreeNode]

  override def relatedNodeNames: Set[String] = childNodeNames

  override def relatedNodeTypes: Set[String] = childNodeTypes

  def childNodeNames: Set[String]

  def childNodeTypes: Set[String]

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
