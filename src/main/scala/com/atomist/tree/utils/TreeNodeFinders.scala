package com.atomist.tree.utils

import com.atomist.tree.{ContainerTreeNode, TerminalTreeNode, TreeNode}

/**
  * Utility methods to find things within a TreeNode structure
  */
object TreeNodeFinders {

  def requiredSingleChild(parent: TreeNode): TreeNode = parent match {
    case ctn: ContainerTreeNode =>
      if (ctn.childNodes.size != 1)
        throw new IllegalArgumentException(s"Found ${ctn.childNodes.size} matches rather than 1 under " +
          s"${parent.nodeName}:${parent.nodeType}[${parent.value}] with children [${ctn.childNodeNames.mkString(",")}]")
      else ctn.childNodes.head
    case x => ???
  }

  def requiredSingleChild(parent: TreeNode, name: String): TreeNode = parent match {
    case ctn: ContainerTreeNode =>
      val hits = ctn.apply(name)
      if (hits.size != 1)
        throw new IllegalArgumentException(s"Found ${hits.size} matches rather than 1 for [$name] in " +
          s"${parent.nodeName}:${parent.nodeType}[${parent.value}] with children [${ctn.childNodeNames.mkString(",")}]")
      else hits.head
    case x => ???
  }

  def singleChild(parent: TreeNode, name: String): Option[TreeNode] = parent match {
    case ctn: ContainerTreeNode =>
      val hits = ctn.apply(name)
      if (hits.size == 1)
        Some(hits.head)
      else None
    case x => None
  }

  def requiredSingleFieldValue(parent: TreeNode, name: String): String = parent match {
    case ctn: ContainerTreeNode =>
      ctn.apply(name) match {
        case Seq(tn: TerminalTreeNode) => tn.value
        case Nil =>  throw new IllegalArgumentException(s"Found no fields for [$name] in $parent: Needed exactly 1")
      }
    case _ => ???
  }

  /**
    * Find all nodes with the given name, at any level within the
    * target structure
    *
    * @param nodeName name of nodes to find
    * @param in       where to look
    * @return sequence of field values
    */
  def findByName(nodeName: String, in: TreeNode): Seq[TreeNode] = in match {
    case ofv: ContainerTreeNode =>
      (ofv.childNodes collect {
        case hasName if hasName.nodeName.equals(nodeName) => Seq(hasName)
        case whatever => findByName(nodeName, whatever)
      }).flatten

    case _ => Nil
  }
}
