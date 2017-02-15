package com.atomist.tree.utils

import com.atomist.tree.TreeNode

/**
  * Utility methods to find things within a TreeNode structure
  */
object TreeNodeFinders {

  def requiredSingleChild(parent: TreeNode, name: String): TreeNode = {
    val hits = parent.childrenNamed(name)
    if (hits.size != 1)
      throw new IllegalArgumentException(s"Found ${hits.size} matches rather than 1 for [$name] in " +
        s"${parent.nodeName}:${parent.nodeTags}[${parent.value}] with children [${parent.childNodeNames.mkString(",")}]")
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
      case Seq(tn) => tn.value
      case s if s.isEmpty => throw new IllegalArgumentException(s"Found no fields for [$name] in $parent: Needed exactly 1")
      case more => throw new IllegalArgumentException(s"Found ${more.size} fields for [$name] in $parent: Needed exactly 1")
    }
  }

}
