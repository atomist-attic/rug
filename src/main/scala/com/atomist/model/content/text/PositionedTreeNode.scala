package com.atomist.model.content.text


trait Positioned {

  def startPosition: InputPosition

  def endPosition: InputPosition
}

/**
  * A TreeNode that knows its position in input.
  */
trait PositionedTreeNode extends TreeNode with Positioned
