package com.atomist.tree.content.text

import com.atomist.tree.TreeNode

trait Positioned {

  def startPosition: InputPosition

  def endPosition: InputPosition
}

/**
  * A TreeNode that knows its position in input.
  */
trait PositionedTreeNode extends TreeNode with Positioned
