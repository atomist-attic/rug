package com.atomist.tree.content.text

import com.atomist.tree.{ContainerTreeNode, TreeNode}
import TreeNodeOperations.NodeTransformer

import scala.collection.mutable.ListBuffer

object ViewTree {

  /**
    * Puts a view over an existing tree
    *
    * @param of tree to put view over
    * @param t  FieldTransformer
    */
  def apply(of: MutableContainerTreeNode, t: NodeTransformer): ViewTree = {

    val filtered1 = of.fieldValues map (tn => {
      val transformed = t(tn)
      transformed.map {
        case ctn: MutableContainerTreeNode => ViewTree(ctn, t)
        case x => x
      }
    })

    val filtered = filtered1.flatten.map(t).flatten
    // TODO this is a bit ugly
    val _fieldValues = ListBuffer.empty[TreeNode]
    _fieldValues.appendAll(filtered)
    new ViewTree(of, filtered)

  }
}

class ViewTree(of: MutableContainerTreeNode, filtered: Seq[TreeNode])
  extends MutableContainerTreeNode {

  def delegate: ContainerTreeNode = of

  override def childrenNamed(key: String): Seq[TreeNode] = filtered.filter(n => n.nodeName.equals(key))

  override def childNodes: Seq[TreeNode] = filtered

  override def childNodeNames: Set[String] = of.childNodeNames

  override def fieldValues: Seq[TreeNode] = filtered

  override def childNodeTypes: Set[String] = filtered.flatMap(fv => fv.nodeType).toSet

  override def nodeName: String = of.nodeName

  addTypes(of.nodeType)

  override def value: String = of.value

  override def update(to: String): Unit = of.update(to)

  override def appendField(newField: TreeNode): Unit = of.appendField(newField)

  override def toString =
    s"${getClass.getSimpleName}($nodeName:$nodeType){${childNodes.mkString(",")}}"

}
