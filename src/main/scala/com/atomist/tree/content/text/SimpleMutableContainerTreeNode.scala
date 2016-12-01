package com.atomist.tree.content.text

import com.atomist.tree.TreeNode

class SimpleMutableContainerTreeNode(
                                      name: String,
                                      val initialFieldValues: Seq[TreeNode],
                                      val startPosition: InputPosition,
                                      val endPosition: InputPosition
                                    )
  extends AbstractMutableContainerTreeNode(name) {

  initialFieldValues.foreach(insertFieldCheckingPosition)
}

object SimpleMutableContainerTreeNode {

  import OffsetInputPosition._

  def wholeInput(name: String, fieldValues: Seq[TreeNode], input: String): MutableContainerTreeNode = {
    val moo = new SimpleMutableContainerTreeNode(name, fieldValues, startOf(input), endOf(input))
    moo.pad(input, true)
    moo
  }
}
