package com.atomist.tree.content.text

import com.atomist.tree.{ContainerTreeNode, MutableTreeNode, TreeNode}
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

  override def dirty: Boolean =
    (childNodes collect {
      case u: MutableTreeNode if u.dirty => u
    }).nonEmpty

  /**
    * Return formatInfo for the start of this child node. We can obtain
    * line and column and formatting information.
    *
    * @param child child of this node
    * @return Some if the child is found
    */
  def formatInfoStart(child: TreeNode): Option[FormatInfo] =
    formatInfoFor(child, start = true)

  /**
    * Return formatInfo for the end of this child node. We can obtain
    * line and column and formatting information.
    *
    * @param child child of this node
    * @return Some if the child is found
    */
  def formatInfoEnd(child: TreeNode): Option[FormatInfo] =
    formatInfoFor(child, start = false)

  protected def formatInfoFor(child: TreeNode, start: Boolean): Option[FormatInfo] = {
    if (!fieldValues.contains(child))
      None
    else {
      // Build string to the left
      val leftFields = fieldValues.takeWhile(f => f != child) ++ (if (start) Nil else Seq(child))
      val stringToLeft = leftFields.map(_.value).mkString("")
      val fi = FormatInfo.contextInfo(stringToLeft)
      //println(s"Found $fi from left string [$stringToLeft] with start=$start")
      Some(fi)
    }
  }

}