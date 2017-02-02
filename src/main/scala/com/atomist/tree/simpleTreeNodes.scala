package com.atomist.tree
import com.atomist.tree.TreeNode.Significance

/**
  * Convenient terminal node implementation.
  * Prefer PaddingNode for padding.
  * @param nodeName name of the field (usually unimportant)
  * @param value field content.
  */
case class SimpleTerminalTreeNode(nodeName: String,
                                  value: String,
                                  types: Set[String] = Set())
  extends TerminalTreeNode {

  override val nodeTags: Set[String] = super.nodeTags ++ types
}

/**
  * Convenient class for padding nodes
  * @param description description of what's being padded
  * @param value padding content
  */
case class PaddingTreeNode(description: String, value: String)
  extends TerminalTreeNode {

  override def significance: Significance = TreeNode.Noise

  override def nodeName = s"padding:$description"

}