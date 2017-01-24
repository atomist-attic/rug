package com.atomist.tree.utils

import com.atomist.tree.{ContainerTreeNode, TerminalTreeNode, TreeNode}

/**
  * Utility methods to find things within a TreeNode structure
  */
object TreeNodeFinders {

  def requiredSingleChild(parent: TreeNode): TreeNode = {
    if (parent.childNodes.size != 1)
      throw new IllegalArgumentException(s"Found ${parent.childNodes.size} matches rather than 1 under " +
        s"${parent.nodeName}:${parent.nodeType}[${parent.value}] with children [${parent.childNodeNames.mkString(",")}]")
    else parent.childNodes.head
  }

  def requiredSingleChild(parent: TreeNode, name: String): TreeNode = {
    val hits = parent.childrenNamed(name)
    if (hits.size != 1)
      throw new IllegalArgumentException(s"Found ${hits.size} matches rather than 1 for [$name] in " +
        s"${parent.nodeName}:${parent.nodeType}[${parent.value}] with children [${parent.childNodeNames.mkString(",")}]")
    else hits.head
  }

  def singleChild(parent: TreeNode, name: String): Option[TreeNode] = {
    val hits = parent.childrenNamed(name)
    if (hits.size == 1)
      Some(hits.head)
    else None
  }

  def requiredSingleFieldValue(parent: TreeNode, name: String): String = {
    parent.childrenNamed(name) match {
      case Seq(tn: TerminalTreeNode) => tn.value
      case Nil => throw new IllegalArgumentException(s"Found no fields for [$name] in $parent: Needed exactly 1")
    }
  }

  /**
    * Find all nodes with the given name, at any level within the
    * target structure
    *
    * @param nodeName name of nodes to find
    * @param in       where to look
    * @return sequence of field values
    */
  def findByName(nodeName: String, in: TreeNode): Seq[TreeNode] =
      (in.childNodes collect {
        case hasName if hasName.nodeName.equals(nodeName) => Seq(hasName)
        case whatever => findByName(nodeName, whatever)
      }).flatten
}
