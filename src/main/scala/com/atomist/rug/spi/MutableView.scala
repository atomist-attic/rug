package com.atomist.rug.spi

import com.atomist.rug.runtime.rugdsl.Evaluator
import com.atomist.tree.{ContainerTreeNode, TreeNode}

/**
  * View read operations.
  *
  * @tparam T type of the underlying object
  */
trait View[T] extends ContainerTreeNode {

  /**
    * Nullable if at the top level, as we don't want to complicate
    * use from JavaScript by using Option.
    */
  def parent: MutableView[_]

  def originalBackingObject: T

  override def childNodes: Seq[TreeNode] =
    childNodeNames.toSeq.flatMap(name => children(name))

  override def value: String = ???

  // TODO not very nice that we need to express children in terms of MutableView, not View, but it's OK for now
  def children(fieldName: String): Seq[MutableView[_]]

}

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
trait MutableView[T] extends View[T] {

  def dirty: Boolean

  def currentBackingObject: T

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
