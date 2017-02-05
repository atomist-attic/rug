package com.atomist.tree

/**
  * Extended by TreeNode implementations with
  * the ability to add additional types to nodes.
  */
trait AnnotatableTreeNode extends TreeNode {

  private var types: Set[String] = Set()

  override def nodeTags: Set[String] = types

  /**
    * Add an additional type to this node. It's impossible
    * to remove a type.
    * @param t type to add
    */
  def addType(t: String): Unit = {
    types = types + t
  }

  /**
    * Add multiple types to this node
    * @param s set of node types
    */
  def addTypes(s: Set[String]): Unit = {
    types = types ++ s
  }

}

/**
  * Extended by TreeNode implementations that allow updates from a string value.
  * Terminal nodes will have their value changed: container nodes
  * will overwrite their child structure.
  * Also adds the ability to add additional types to nodes.
  */
trait MutableTreeNode extends AnnotatableTreeNode {

  /**
    * Update String contents to this content. May
    * involve updating an entire structure.
    *
    * @return
    */
  def update(to: String): Unit

  def dirty: Boolean

}