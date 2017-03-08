package com.atomist.tree.marshal

import com.atomist.rug.ts.{Cardinality, OneToM}
import com.atomist.rug.spi.ExportFunction
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
}

/**
  * A node has been encountered in the path expression that cannot be resolved by the materializing
  * implementation. A suitable interface by be able to continue to resolve further. This class
  * is the parent class of such nodes and usually should be extended.
  *
  * As an example a NeoTreeMaterializer is able to resolve across the Neo database but when it finds
  * a repo with folders is unable to proceed further down that path. It will return an UnresolvableNode with the
  * remaining path expression as a property.
  *
  * @param remainingPathExpression The parts of the path expression that were unable to be resolved
  */
class UnresolvableNode(val remainingPathExpression: String)
  extends ContainerTreeNode {

  @ExportFunction(readOnly = true, description = "Node content")
  override def value: String = ???

  override def childrenNamed(key: String): Seq[TreeNode] = ???

  override def childNodeNames: Set[String] = ???

  override def childNodeTypes: Set[String] = ???

  @ExportFunction(readOnly = true, description = "Name of the node")
  override def nodeName: String = "UnresolveableNode"
}

/**
  * An unresolvedable node representing a Project with remaining path expression provided. Also
  * contains suitable context to proceed further into the path expression
  *
  * @param remainingPathExpression the remaining parts of the path expression to resolve
  * @param vcsOrg the organization of the project in source control
  * @param vcsRepo the repository we are attempting to enter, in source control
  * @param sha (optional) the sha of the commit that represents the point in history and branches that we
  *            are resolving the path expression for. If not provided we should assume head of master
  */
class UnresolvableProjectNode(override val remainingPathExpression: String,
                              val vcsOrg: String,
                              val vcsRepo: String,
                              val sha: String = null)
  extends UnresolvableNode(remainingPathExpression) {

}
