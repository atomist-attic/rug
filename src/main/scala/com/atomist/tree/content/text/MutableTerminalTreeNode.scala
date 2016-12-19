package com.atomist.tree.content.text

import com.atomist.rug.spi.{ExportFunction, TypeProvider}
import com.atomist.tree.{MutableTreeNode, TerminalTreeNode}

class MutableTerminalTreeNodeTypeProvider
  extends TypeProvider(classOf[MutableTerminalTreeNode]) {

  override def description: String = "Updateable terminal node"
}

/**
  * Updateable terminal node.
  *
  * @param nodeName name of the field
  */
class MutableTerminalTreeNode(
                               val nodeName: String,
                               val initialValue: String,
                               val startPosition: InputPosition)
  extends TerminalTreeNode
    with PositionedTreeNode
    with MutableTreeNode {

  def this(other: MutableTerminalTreeNode) = {
    this(other.nodeName, other.initialValue, other.startPosition)
  }

  override def nodeType: String = "MutableTerminal"

  private var currentValue = initialValue

  @ExportFunction(readOnly = false, description = "Update the node value")
  override def update(newValue: String): Unit = {
    currentValue = newValue
  }

  override def endPosition: InputPosition = startPosition + currentValue.length

  @ExportFunction(readOnly = true, description = "Return the value")
  override def value = currentValue

  override def dirty = currentValue != initialValue

  def longString =
    s"scalar:${getClass.getSimpleName}: $nodeName=[$currentValue], position=$startPosition"

  override def toString =
    s"[scalar:name='$nodeName'; value='$currentValue'; $startPosition]"
}
