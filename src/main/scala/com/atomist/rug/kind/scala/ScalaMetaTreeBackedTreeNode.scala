package com.atomist.rug.kind.scala

import com.atomist.tree.TreeNode
import com.atomist.tree.content.text.{InputPosition, OffsetInputPosition, PositionedTreeNode}

import scala.meta._

/**
  * Immutable tree backed by a ScalaMeta Tree
  */
private[scala] class ScalaMetaTreeBackedTreeNode(smTree: Tree)
  extends TreeNode with PositionedTreeNode {

  override val nodeName: String = {
    val fqn = smTree.getClass.getName
    fqn.drop(fqn.lastIndexOf("$") + 1).replace("Impl", "")
  }

  override def startPosition: InputPosition = OffsetInputPosition(smTree.pos.start.offset)

  override def endPosition: InputPosition = OffsetInputPosition(smTree.pos.end.offset)

  override def value: String = smTree.syntax

  def childNodeNames: Set[String] = children.map(_.nodeName).toSet

  override def childNodeTypes: Set[String] = childNodes.flatMap(n => n.nodeTags).toSet

  def children: Seq[TreeNode] = smTree.children.map(new ScalaMetaTreeBackedTreeNode(_))

  override def childrenNamed(key: String): Seq[TreeNode] = children.filter(_.nodeName == key)

  override def toString: String = s"$nodeName:${nodeTags.mkString(",")}:[$value]"

}
