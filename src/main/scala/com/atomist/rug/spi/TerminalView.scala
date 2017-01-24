package com.atomist.rug.spi

import com.atomist.tree.TreeNode

trait TerminalView[T] extends MutableView[T] {

  override def childNodeTypes: Set[String] = Set()

  override def childrenNamed(key: String): Seq[TreeNode] = Nil
}
