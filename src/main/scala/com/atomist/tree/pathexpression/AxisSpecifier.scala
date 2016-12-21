package com.atomist.tree.pathexpression

import com.atomist.tree.{ContainerTreeNode, TreeNode}
import com.atomist.util.{Visitable, Visitor}

import scala.collection.mutable.ListBuffer

/**
  * Inspire by XPath axis specifier concept. Represents a direction
  * of navigation rom a node.
  */
trait AxisSpecifier {

  override def toString: String = this.getClass.getSimpleName
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
  def allDescendants(tn: TreeNode): Seq[TreeNode] = tn match {
    case ctn: ContainerTreeNode =>
      val v = new SaveAllDescendantsVisitor
      ctn.accept(v, 0)
      // Remove this node
      v.nodes.diff(Seq(tn))
    case x => Nil
  }

  private class SaveAllDescendantsVisitor extends Visitor {

    private val _nodes = ListBuffer.empty[TreeNode]

    override def visit(v: Visitable, depth: Int): Boolean = v match {
      //    case ctn: ContainerTreeNode =>
      //      true
      case tn: TreeNode =>
        _nodes.append(tn)
        true
    }

    def nodes: Seq[TreeNode] = _nodes
  }
}

object Attribute extends AxisSpecifier
