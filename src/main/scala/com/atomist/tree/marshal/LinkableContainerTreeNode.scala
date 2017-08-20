package com.atomist.tree.marshal

import com.atomist.rug.ts.{Cardinality, OneToM}
import com.atomist.tree.{ContainerTreeNode, TreeNode}

private class LinkableContainerTreeNode(
                                         val nodeName: String,
                                         override val nodeTags: Set[String],
                                         private var fieldValues: Seq[TreeNode]
                                       )
  extends ContainerTreeNode {

  def link(c: LinkableContainerTreeNode, link: String, cardinality: Cardinality): Unit = {
    // Add a child with the appropriate name
    val nn = new WrappingLinkableContainerTreeNode(c, link, cardinality)
    fieldValues = fieldValues :+ nn
  }

  override def childNodeNames: Set[String] =
    fieldValues.map(f => f.nodeName).toSet

  override def childNodeTypes: Set[String] =
    fieldValues.flatMap(f => f.nodeTags).toSet

  override def value: String = ???

  override def childrenNamed(key: String): Seq[TreeNode] =
    fieldValues.filter(n => n.nodeName.equals(key))

  // WARNING - this is a graph not a tree. We should expect cycles. As such it's not safe
  // to call toString on our child nodes
  override def toString: String = {
    getClass.getSimpleName + s": (${nodeTags.mkString(",")}); " +
      childNodeNames.mkString("; ")
  }
}

private class WrappingLinkableContainerTreeNode(val wrappedNode: LinkableContainerTreeNode,
                                                override val nodeName: String,
                                                cardinality: Cardinality)
  extends ContainerTreeNode {

  override def value: String = wrappedNode.value

  override def nodeTags: Set[String] = wrappedNode.nodeTags ++ (cardinality match {
    case OneToM => Set(Cardinality.One2Many)
    case _ => Set()
  })

  override def childNodeNames: Set[String] = wrappedNode.childNodeNames

  override def childNodeTypes: Set[String] = wrappedNode.childNodeTypes

  override def childrenNamed(key: String): Seq[TreeNode] = wrappedNode.childrenNamed(key)

  override def toString: String = wrappedNode.toString
}
