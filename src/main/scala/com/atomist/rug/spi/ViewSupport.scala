package com.atomist.rug.spi

import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.tree.TreeNode

import scala.collection.mutable.ListBuffer

abstract class ViewSupport[T](val originalBackingObject: T, val parent: MutableView[_])
  extends CommonViewOperations[T] {

  private var _currentBackingObject: T = originalBackingObject

  private var _previousBackingObject: T = originalBackingObject

  private val updaters: ListBuffer[Updater[T]] = new ListBuffer()

  private var _dirty: Boolean = false

  override def currentBackingObject: T = _currentBackingObject

  override def dirty: Boolean = _dirty

  def previousBackingObject: T = _previousBackingObject

  /**
    * Subclasses can call this to update the state of this object.
    */
  override def updateTo(newBackingObject: T): Unit = {
    _dirty = true
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

  override val evaluator: Evaluator =
    new DefaultEvaluator(ReflectiveFunctionExport.exportedRegistry(getClass))
}

abstract class TreeViewSupport[T <: TreeNode](originalBackingObject: T, parent: MutableView[_])
  extends ViewSupport(originalBackingObject, parent) {

  override def nodeName: String = currentBackingObject.nodeName

  override def value: String = currentBackingObject.value

  override def nodeType: Set[String] = currentBackingObject.nodeType

}
