package com.atomist.tree

/**
  * Extended by TreeNode implementations that allow updates from a string value.
  * Terminal nodes will have their value changed: container nodes
  * will overwrite their child structure.
  */
trait MutableTreeNode extends TreeNode {

  /**
    * Update String contents to this content. May
    * involve updating an entire structure.
    *
    * @return
    */
  def update(to: String): Unit

  def dirty: Boolean

}