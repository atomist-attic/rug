package com.atomist.model.content.text

import com.typesafe.scalalogging.LazyLogging

/**
  * ContainerTreeNode that allows updates.
  */
trait MutableContainerTreeNode
  extends ContainerTreeNode
    with MutableTreeNode
    with LazyLogging {

  def fieldValues: Seq[TreeNode]

  def appendField(newField: TreeNode): Unit

  def appendFields(newFields: Seq[TreeNode]): Unit = {
    newFields.foreach(appendField)
  }
}