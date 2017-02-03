package com.atomist.tree.content.text

import com.atomist.tree.TreeNode

trait PositionedPaddableTreeNode extends PositionedTreeNode {

  def initialized: Boolean = startPosition != null && startPosition.offset >= 0

  def padded: Boolean

  /**
    * Compile this so that we can manipulate it at will without further
    * reference to the input string.
    * Introduces padding objects to cover string content that isn't explained in known structures.
    * Must be called before value method is invoked.
    *
    * @param initialSource entire source
    * @param noiseFilter function that determines whether a node is noise
    * @param topLevel      whether this is a top level element, in which
    *                      case we should pad after known structures
    */
  def pad(initialSource: String,
          noiseFilter: TreeNode => Boolean = n => n.significance == TreeNode.Noise,
          topLevel: Boolean = false): Unit

}
