package com.atomist.rug.kind.dynamic

import com.atomist.rug.spi.{ExportFunction, ExportFunctionParameterDescription, MutableView, ViewSupport}
import com.atomist.tree.content.text._
import com.atomist.tree.{ContainerTreeNode, MutableTreeNode, TreeNode}

/**
  * Allows select (with/from) navigation over any structured data that can
  * be put into our TreeNode tree structure.
  */
class ContainerTreeNodeView[O <: ContainerTreeNode](
                                                     originalBackingObject: O,
                                                     parent: MutableView[_])
  extends ViewSupport[O](originalBackingObject, parent)
    with FormatInfoProviderSupport {

  override def nodeName: String = currentBackingObject.nodeName

  override def nodeTags: Set[String] = currentBackingObject.nodeTags

  @ExportFunction(readOnly = true, description = "Value")
  override def value: String = currentBackingObject.value

  override def dirty: Boolean = false

  override protected def focusNode: TreeNode = currentBackingObject

  @ExportFunction(readOnly = true, description = "Return the value of the given key")
  def valueOf(@ExportFunctionParameterDescription(name = "name",
    description = "The match key whose content you want")
              name: String): Object = {
    originalBackingObject.childrenNamed(name).toList match {
      case Nil => ???
      case List(f: TreeNode) => f.value
      case _ => ???
    }
    //.getOrElse("")
  }

  override def childNodeNames: Set[String] = currentBackingObject.childNodeNames

  override def childNodeTypes: Set[String] = currentBackingObject.childNodeTypes

  override def childrenNamed(fieldName: String): Seq[MutableView[_]] =
    currentBackingObject.childrenNamed(fieldName) collect {
      case o: ContainerTreeNode => viewFrom(o)
      case sv: MutableTerminalTreeNode => new ScalarValueView(sv, this)
    }

  /**
    * Subclasses can override this if they want to return mutable views.
    */
  protected def viewFrom(o: ContainerTreeNode): ContainerTreeNodeView[_] = {
    new ContainerTreeNodeView(o, this)
  }

  override def toString = s"${getClass.getSimpleName} wrapping $currentBackingObject"
}

class ScalarValueView(
                       originalBackingObject: MutableTerminalTreeNode,
                       parent: MutableView[_])
  extends ViewSupport[MutableTerminalTreeNode](originalBackingObject, parent)
    with MutableTreeNode
    with FormatInfoProviderSupport {

  override def dirty: Boolean = originalBackingObject.dirty

  addTypes(currentBackingObject.nodeTags ++ Set("MutableTerminal"))

  override protected def focusNode: TreeNode = currentBackingObject

  override def nodeName: String = originalBackingObject.nodeName

  override def value: String = originalBackingObject.value

  @ExportFunction(readOnly = false, description = "Update the value of the sole key")
  override def update(to: String): Unit = setValue(to)

  @ExportFunction(readOnly = true, description = "Return the value of the sole key")
  def valueOf: String = originalBackingObject.value

  @ExportFunction(readOnly = false, description = "Update the value of the sole key")
  def setValue(@ExportFunctionParameterDescription(name = "name",
    description = "The new value") newValue: String): Unit = {
    originalBackingObject.update(newValue)
    commit()
  }

  override val childNodeNames: Set[String] = Set(originalBackingObject.nodeName)

  override def childNodeTypes: Set[String] = Set()

  override def childrenNamed(fieldName: String): Seq[MutableView[_]] = Nil

  override def toString = s"${getClass.getSimpleName} wrapping $currentBackingObject"
}
