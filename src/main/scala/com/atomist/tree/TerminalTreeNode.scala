package com.atomist.tree

import com.atomist.util.Visitor

/**
  * Convenient supertrait for terminal TreeNodes. Contains a simple string value.
  */
trait TerminalTreeNode extends TreeNode {

  final override def accept(v: Visitor, depth: Int): Unit = v.visit(this, depth)

  final override def childNodeNames: Set[String] = Set()

  final override def childNodeTypes: Set[String] = Set()

  final override def childrenNamed(key: String): Seq[TreeNode] = Nil

}
