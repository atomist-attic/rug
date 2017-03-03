package com.atomist.rug.spi

import com.atomist.rug.kind.core.{ChangeCounting, FileArtifactBackedMutableView}
import com.atomist.tree.TreeNode
import com.atomist.tree.content.text.{MutableContainerTreeNode, PositionedMutableContainerTreeNode}

import scala.collection.mutable.ListBuffer

abstract class ViewSupport[T](val originalBackingObject: T, val parent: MutableView[_])
  extends CommonViewOperations[T]
    with ChangeCounting {

  private var _currentBackingObject: T = originalBackingObject

  private var _previousBackingObject: T = originalBackingObject

  private val updaters: ListBuffer[Updater[T]] = new ListBuffer()

  override def currentBackingObject: T = _currentBackingObject

  def previousBackingObject: T = _previousBackingObject

  private var _changeCount: Int = 0

  override def changeCount: Int = _changeCount

  override def dirty: Boolean = changeCount > 0

  // Implementation of FormatInfoProvider relevant if subclasses choose to implement that trait
  protected def rootNode: Option[MutableContainerTreeNode] = {
    def highestNodeWithinFile(mv: MutableView[_]): Option[MutableView[_]] = {
      if (mv.parent == null) None // We failed
      else if (mv.parent.isInstanceOf[FileArtifactBackedMutableView]) {
        Some(mv)
      }
      else highestNodeWithinFile(mv.parent)
    }

    val hnwf = highestNodeWithinFile(this)
    hnwf.flatMap(mv => mv.originalBackingObject match {
      case pmct: PositionedMutableContainerTreeNode => Some(pmct)
      case _ => None
    })
  }

  /**
    * Subclasses can call this to update the state of this object.
    */
  override def updateTo(newBackingObject: T): Unit = {
    _changeCount += 1
    _previousBackingObject = _currentBackingObject
    _currentBackingObject = newBackingObject
    if (parent != null)
      updateParent()
  }

  /**
    * Update the parent after changing this class. Subclasses can override
    * this implementation, which does nothing.
    */
  protected def updateParent(): Unit = {
  }

  override def registerUpdater(u: Updater[T]): Unit = updaters.append(u)

  /**
    * Convenience method to register an update function.
    */
  def registerUpdater(u: MutableView[T] => Unit): Unit =
    registerUpdater(new Updater[T] {
      override def update(v: MutableView[T]): Unit = u(v)
    })

  /**
    * Commit all changes, invoking updaters and calling parent if necessary.
    */
  override def commit(): Unit = {
    updaters.foreach(u => u.update(this))
    if (parent != null)
      parent.commit()
  }

}

abstract class TreeViewSupport[T <: TreeNode](originalBackingObject: T, parent: MutableView[_])
  extends ViewSupport(originalBackingObject, parent) {

  override def nodeName: String = currentBackingObject.nodeName

  override def value: String = currentBackingObject.value

  override def nodeTags: Set[String] = currentBackingObject.nodeTags

}
