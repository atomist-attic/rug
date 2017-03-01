package com.atomist.tree.marshal

import com.atomist.tree.{ContainerTreeNode, TreeNode}

class EmptyLinkableContainerTreeNode() extends ContainerTreeNode {

  override def nodeName: String = "empty"

  // Using null over Option as this is part of the interface to JavaScript
  override def value: String = null

  override def nodeTags: Set[String] = Set.empty

  override def childNodeNames: Set[String] = Set.empty

  override def childNodeTypes: Set[String] = Set.empty

  override def childrenNamed(key: String): Seq[TreeNode] = Nil
}

private class LinkableContainerTreeNode(
                                         val nodeName: String,
                                         override val nodeTags: Set[String],
                                         private var fieldValues: Seq[TreeNode]
                                       )
  extends ContainerTreeNode {

  def link(c: LinkableContainerTreeNode, link: String): Unit = {
    // Add a child with the appropriate name
    val nn = new WrappingLinkableContainerTreeNode(c, link)
    fieldValues = fieldValues :+ nn
  }

  override def childNodeNames: Set[String] =
    fieldValues.map(f => f.nodeName).toSet

  override def childNodeTypes: Set[String] =
    fieldValues.flatMap(f => f.nodeTags).toSet

  override def value: String = ???

  override def childrenNamed(key: String): Seq[TreeNode] =
    fieldValues.filter(n => n.nodeName.equals(key))
}

private class WrappingLinkableContainerTreeNode(val wrappedNode: LinkableContainerTreeNode,
                                                override val nodeName: String)
  extends ContainerTreeNode {

  override def value: String = wrappedNode.value

  override def nodeTags: Set[String] = wrappedNode.nodeTags

  override def childNodeNames: Set[String] = wrappedNode.childNodeNames

  override def childNodeTypes: Set[String] = wrappedNode.childNodeTypes

  override def childrenNamed(key: String): Seq[TreeNode] = wrappedNode.childrenNamed(key)
}
