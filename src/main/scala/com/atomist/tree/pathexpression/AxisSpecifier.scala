package com.atomist.tree.pathexpression

import com.atomist.graph.GraphNode
import com.atomist.tree.{ContainerTreeNode, TreeNode}
import com.atomist.util.{Visitable, Visitor}
import com.fasterxml.jackson.annotation.JsonProperty

import scala.collection.mutable.ListBuffer

/**
  * Inspire by XPath axis specifier concept. Represents a direction
  * of navigation from a node.
  */
trait AxisSpecifier {

  @JsonProperty
  def name: String = {
    // Get rid of the trailing $ from an object
    getClass.getSimpleName.replace("$", "")
  }

  override def toString: String = name

}

/**
  * The current node
  */
object Self extends AxisSpecifier

/**
  * Any child of the current node
  */
object Child extends AxisSpecifier

/**
  * Any descendant of the current node.
  */
object Descendant extends AxisSpecifier {

  // TODO this is very inefficient and needs to be optimized.
  // Subclasses can help, or knowing a plan
  def allDescendants(tn: GraphNode): Seq[GraphNode] = {
    // Remove this node
    selfAndAllDescendants(tn).diff(Seq(tn))
  }

  def selfAndAllDescendants(tn: GraphNode): Seq[GraphNode] = {
    val v = new SaveAllDescendantsVisitor
    tn.accept(v, 0)
    v.nodes
  }

  private class SaveAllDescendantsVisitor extends Visitor {

    private val _nodes = ListBuffer.empty[TreeNode]

    override def visit(v: Visitable, depth: Int): Boolean = v match {
      case tn: TreeNode =>
        _nodes.append(tn)
        true
    }

    def nodes: Seq[TreeNode] = _nodes.distinct
  }

}

object Attribute extends AxisSpecifier

/**
  * Navigation via the node property with the given name
  *
  * @param propertyName name to navigate into
  */
case class NavigationAxis(propertyName: String)
  extends AxisSpecifier
