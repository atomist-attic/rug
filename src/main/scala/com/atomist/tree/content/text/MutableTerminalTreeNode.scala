package com.atomist.tree.content.text

import com.atomist.rug.spi.{ExportFunction, TypeProvider}
import com.atomist.tree.TreeNode.{Signal, Significance, Noise, Undeclared}
import com.atomist.tree.{MutableTreeNode, TerminalTreeNode, TreeNode}

class MutableTerminalTreeNodeTypeProvider
  extends TypeProvider(classOf[MutableTerminalTreeNode]) {

  override def description: String = "Updateable terminal node"
}

/**
  * Updateable terminal node.
  *
  * @param nodeName name of the field
  */
class MutableTerminalTreeNode(val nodeName: String,
                              val initialValue: String,
                              val startPosition: InputPosition,
                              override val significance: Significance = TreeNode.Signal)
  extends TerminalTreeNode
    with PositionedTreeNode
    with MutableTreeNode {

  def this(other: MutableTerminalTreeNode) = {
    this(other.nodeName, other.initialValue, other.startPosition)
  }

  private var currentValue = initialValue

  override def padded: Boolean = true

  override def pad(initialSource: String, topLevel: Boolean): Unit = {}

  @ExportFunction(readOnly = false, description = "Update the node value")
  override def update(newValue: String): Unit =
    currentValue = newValue

  override def endPosition: InputPosition = startPosition + currentValue.length

  @ExportFunction(readOnly = true, description = "Return the value")
  override def value: String = currentValue

  override def dirty: Boolean = currentValue != initialValue

  def longString =
    s"scalar:${getClass.getSimpleName}: $nodeName=[$currentValue], position=$startPosition"

  override def toString = {
    val sig = significance match {
      case Noise => "noise "
      case Signal => "signal "
      case Undeclared => ""
    }
    s"[${sig}scalar:name='$nodeName'; value='$currentValue'; $startPosition]"
  }
}
