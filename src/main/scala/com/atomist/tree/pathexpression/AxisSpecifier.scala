package com.atomist.tree.pathexpression

import com.atomist.tree.{ContainerTreeNode, TreeNode}
import com.atomist.util.{Visitable, Visitor}

import scala.collection.mutable.ListBuffer

sealed trait AxisSpecifier {

  override def toString = this.getClass.getSimpleName
}

object Self extends AxisSpecifier

object Child extends AxisSpecifier

object Descendant extends AxisSpecifier {

  // TODO this is horribly inefficient.
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

object DescendantOrSelf extends AxisSpecifier