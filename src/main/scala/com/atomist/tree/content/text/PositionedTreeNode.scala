package com.atomist.tree.content.text

import com.atomist.rug.kind.grammar.ParsedNode
import com.atomist.tree.TreeNode

/**
  * These are the beginnings of TreeNodes. They come in from parsing
  * They can't be updated, and shouldn't really know their value.
  *
  * Goal: these should not implement TreeNode. They aren't path-expression-ready
  * they have to go through TextTreeNodeLifecycle first, to be turned into
  * proper TextTreeNodes that know value and can be updated. (Those maintain
  * position using padding instead of offsets, so the updates work.)
  *
  * ParsedNode is the minimal interface that Rug Language Extensions must supply when
  * parsing a file. It's implemented here for backwards compatibility, so that
  * existing extensions which supply full PositionedTreeNodes can continue to do so.
  */
trait PositionedTreeNode extends TreeNode with Positioned with ParsedNode {

  override def parsedNodes: Seq[ParsedNode] = childNodes.map(_.asInstanceOf[ParsedNode])

  override def startOffset: Int = startPosition.offset

  override def endOffset: Int = endPosition.offset

  def hasSamePositionAs(that: PositionedTreeNode): Boolean =
    this.startPosition.offset == that.startPosition.offset

  def includes(pos: InputPosition): Boolean =
    this.startPosition.offset <= pos.offset && this.endPosition.offset >= pos.offset
}

object PositionedTreeNode {
  def fromParsedNode(ptn: ParsedNode): PositionedTreeNode = ptn match {
    case alreadyThere: PositionedTreeNode => alreadyThere
    case _ =>
      ImmutablePositionedTreeNode.apply(ptn.nodeName,
        OffsetInputPosition(ptn.startOffset),
        OffsetInputPosition(ptn.endOffset),
        ptn.parsedNodes.map(fromParsedNode),
        Set(),
        TreeNode.Signal)
  }
}