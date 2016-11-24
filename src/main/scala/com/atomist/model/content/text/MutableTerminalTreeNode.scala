package com.atomist.model.content.text

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

  override def nodeType: String = "mutable"

  private var currentValue = initialValue

  override def update(newValue: String): Unit = {
    currentValue = newValue
  }

  override def endPosition: InputPosition = startPosition + currentValue.length

  override def value = currentValue

  override def dirty = currentValue != initialValue

  def longString =
    s"scalar:${getClass.getSimpleName}: $nodeName=[$currentValue], position=$startPosition"

  override def toString =
    s"[scalar:name='$nodeName'; value='$currentValue'; $startPosition]"
}
