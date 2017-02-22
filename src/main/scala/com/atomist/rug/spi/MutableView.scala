package com.atomist.rug.spi

import com.atomist.rug.runtime.rugdsl.Evaluator
import com.atomist.tree.{ContainerTreeNode, PathAwareTreeNode}

/**
  * Exposed to Rug "with" and "from" blocks, and access from JavaScript.
  * Typically backed by an object from another hierarchy.
  * View implementations will create their own methods for updates
  *
  * Methods annotated with @ExportFunction are exposed via views. Predicates
  * should return Boolean, transform functions the view type.
  * Methods can declare whatever parameters they need from local arguments in the
  * rug script. An optional additional argument if declared will be populated with the
  * Rug FunctionInvocationContext.
  *
  * Return types should be Java, rather than Scala, as they may
  * be invoked from JavaScript via Nashorn.
  *
  * @tparam  T type of the underlying object
  */
trait MutableView[T] extends PathAwareTreeNode with ContainerTreeNode {

  def originalBackingObject: T

  def addressableBackingObject: Any = originalBackingObject

  def dirty: Boolean

  def currentBackingObject: T

  @ExportFunction(readOnly = true, description = "Node content")
  override def value: String = currentBackingObject.toString

  override def parent: MutableView[_]

  /**
    * Subclasses can call this to update the state of this object.
    *
    * @param newBackingObject type of the underlying object
    */
  def updateTo(newBackingObject: T): Unit

  /**
    * Register an updater that will be invoked before a call to currentObject.
    */
  def registerUpdater(u: Updater[T])

  def evaluator: Evaluator

  /**
    * Commit all changes, invoking updaters and calling parent if necessary.
    */
  def commit(): Unit
}

/**
  * Used when we need to update a MutableView from a number of objects,
  * in a particular order.
  *
  * @tparam T type of the underlying object
  */
trait Updater[T] {

  /**
    * Make updates to V.
    */
  def update(v: MutableView[T]): Unit
}
