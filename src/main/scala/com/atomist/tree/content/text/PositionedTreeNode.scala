package com.atomist.tree.content.text

import com.atomist.tree.TreeNode

trait Positioned {

  def startPosition: InputPosition

  def endPosition: InputPosition
}

/**
  * A TreeNode that knows its position in input.
  */
trait PositionedTreeNode extends TreeNode with Positioned {

  def padded: Boolean

  /**
    * Compile this so that we can manipulate it at will without further
    * reference to the input string.
    * Introduces padding objects to cover string content that isn't explained in known structures.
    * Must be called before value method is invoked.
    *
    * @param initialSource entire source
    * @param topLevel      whether this is a top level element, in which
    *                      case we should pad after known structures
    */
  def pad(initialSource: CharSequence, topLevel: Boolean = false, padAtBeginning: Boolean = false): Unit
}
