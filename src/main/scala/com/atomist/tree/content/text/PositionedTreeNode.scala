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
