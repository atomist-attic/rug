package com.atomist.tree.content.text

import com.atomist.tree.TreeNode

/**
  * A TreeNode that knows its position in input.
  */
trait PositionedTreeNode extends TreeNode with Positioned {

  def hasSamePositionAs(that: PositionedTreeNode): Boolean =
    this.startPosition.offset == that.startPosition.offset

  def includes(pos: InputPosition): Boolean =
    this.startPosition.offset <= pos.offset && this.endPosition.offset >= pos.offset

}

/**
  * As a Rug language extension, extend this class for the
  * easiest path to integration.
  * Return these from your rawNodeToFile method.
  */
trait MinimalPositionedTreeNode extends PositionedTreeNode {

  def name: String
  def start: Int
  def end: Int
  def valueOption: Option[String] = None
  def childNodes: Seq[TreeNode]

  override val nodeName: String = name

  override val childNodeNames: Set[String] = childNodes.map(_.nodeName).toSet

  override val childNodeTypes: Set[String] = Set()

  override def childrenNamed(key: String): Seq[TreeNode] = childNodes.filter(_.nodeName == key)

  override val value: String = valueOption.getOrElse("<unavailable>")

  override val startPosition: InputPosition = OffsetInputPosition(start)

  override val endPosition: InputPosition = OffsetInputPosition(end)
}
