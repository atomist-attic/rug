package com.atomist.rug.spi

import com.atomist.rug.runtime.rugdsl.Evaluator
import com.atomist.tree.ContainerTreeNode


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
trait MutableView[T] extends ContainerTreeNode {

  /**
    * Nullable if at the top level, as we don't want to complicate
    * use from JavaScript by using Option.
    */
  def parent: MutableView[_]

  def originalBackingObject: T

  def addressableBackingObject: Any = originalBackingObject

  override def value: String = toString

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

  /* really this should be a path expression but let's start somewhere */
  def address: String = MutableView.address(this, s"name=$nodeName")

}

object MutableView {
  def address(nodeOfInterest: MutableView[_], test: String): String = {
    val myType = nodeOfInterest.nodeTags.mkString(",")
    if (nodeOfInterest.parent == null)
      s"$myType()$test"
    else
      s"${nodeOfInterest.parent.address}/$myType()[$test]"
  }
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
