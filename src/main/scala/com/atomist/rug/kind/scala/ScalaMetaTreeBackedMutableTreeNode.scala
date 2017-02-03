package com.atomist.rug.kind.scala

import com.atomist.tree.TreeNode
import com.atomist.tree.content.text.MutableContainerTreeNode

import scala.language.reflectiveCalls
import scala.meta._

/**
  * Uses copy on write pattern to expose an updateable tree node
  * @param initialTree initial tree. May be replaced
  */
class ScalaMetaTreeBackedMutableTreeNode(initialTree: Tree) extends MutableContainerTreeNode {

  private var currentTree: Tree = initialTree

  override val nodeName: String = {
    val fqn = currentTree.getClass.getName
    fqn.drop(fqn.lastIndexOf("$") + 1).replace("Impl", "")
  }

  override def value: String = currentTree.syntax

  def childNodeNames: Set[String] = children.map(_.nodeName).toSet

  override def childNodeTypes: Set[String] = childNodes.flatMap(n => n.nodeTags).toSet

  def children: Seq[TreeNode] = currentTree.children.map(new ScalaMetaTreeBackedMutableTreeNode(_))

  override def childrenNamed(key: String): Seq[TreeNode] = children.filter(_.nodeName == key)

  override def update(to: String): Unit = {
    currentTree match {
      case n: ({def copy(s: String): Tree})@unchecked =>
        currentTree = n.copy(to)
    }
  }

  override def dirty: Boolean = currentTree != initialTree

  override def fieldValues: Seq[TreeNode] = childNodes

  override def appendField(newField: TreeNode): Unit = ???

  override def toString: String = s"$nodeName:${nodeTags.mkString(",")}:[$value]"
}
