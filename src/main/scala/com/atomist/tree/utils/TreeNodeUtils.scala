package com.atomist.tree.utils

import com.atomist.tree.content.text.{AbstractMutableContainerTreeNode, PositionedTreeNode}
import com.atomist.tree.{ContainerTreeNode, TreeNode}

/**
  * Utility methods for TreeNode instances
  */
object TreeNodeUtils {

  /**
    * Replace returns in values with a character that's easier to read in a debug log
    *
    * @param s
    * @return
    */
  def inlineReturns(s: String) = s.replace("\n", "ø")

  def toShortString(fv: TreeNode): String = {

    def tabs(n: Int) = List.fill(n)("\t").mkString("")

    def offset(fv: TreeNode): String = fv match {
      case pf: PositionedTreeNode => s"${pf.startPosition.offset}-${pf.endPosition.offset}"
      case _ => ""
    }

    def showValue(n: TreeNode, cutoff: Int) = inlineReturns(
      n match {
        case cn: AbstractMutableContainerTreeNode if !cn.padded => ""
        case n if n.value.length < cutoff => n.value
        case n => n.value.take(cutoff) + "..."
      })

    def info(f: TreeNode) =
      s"${f.nodeName} (${f.getClass.getSimpleName}#${f.hashCode()}) ${offset(f)}:[${showValue(f, 50)}]"

    def toShortStr(fv: TreeNode, depth: Int): String = fv match {
      case ofv: ContainerTreeNode =>
        tabs(depth) + info(ofv) + (if (ofv.childNodes.nonEmpty) ":\n" else "") + ofv.childNodes.map(toShortStr(_, depth + 1)).mkString("\n")
      case f => tabs(depth) + info(f) + "\n"
    }

    toShortStr(fv, 0)
  }
}
