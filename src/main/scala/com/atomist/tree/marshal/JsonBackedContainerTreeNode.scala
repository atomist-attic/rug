package com.atomist.tree.marshal

import com.atomist.tree.{ContainerTreeNode, TreeNode}

/**
  * Allows us to return a ContainerTreeNode that encapsulates a json representation
  * of the object tree.
  *
  * @param innerNode The ContainerTreeNode that we are encapsulating
  * @param backingJson the json string that also represents this object tree
  */
class JsonBackedContainerTreeNode(val innerNode: ContainerTreeNode,
                                  val backingJson: String,
                                  val version: String)
  extends ContainerTreeNode {

  override def value: String = innerNode.value

  override def nodeName: String = innerNode.nodeName

  override def nodeTags: Set[String] = innerNode.nodeTags

  override def childNodeNames: Set[String] = innerNode.childNodeNames

  override def childNodeTypes: Set[String] = innerNode.childNodeTypes

  override def childrenNamed(key: String): Seq[TreeNode] = innerNode.childrenNamed(key)

  def jsonRepresentation: String = backingJson
}
