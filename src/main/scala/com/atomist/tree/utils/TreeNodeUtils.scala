package com.atomist.tree.utils

import com.atomist.graph.GraphNode
import com.atomist.tree.TreeNode.{Noise, Signal, Undeclared}
import com.atomist.tree.content.text.{PositionedMutableContainerTreeNode, PositionedTreeNode}
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
  def inlineReturns(s: String): String = s.replace("\n", "ø")

  /**
    * Return a directory tree like representation of the node, using newlines and tabs
    * to show nesting and structure. For use in diagnostics.
    *
    * @param tn node to represent
    * @return string representation of the node
    */
  def toShortString(tn: GraphNode): String = {

    def tabs(n: Int) = List.fill(n)("\t").mkString("")

    def offset(fv: GraphNode): String = fv match {
      case pf: PositionedTreeNode => s"${pf.startPosition.offset}-${pf.endPosition.offset}"
      case _ => ""
    }

    def showValue(n: GraphNode, cutoff: Int) = inlineReturns(
      n match {
        case cn: PositionedMutableContainerTreeNode if !cn.padded => ""
        case n: TreeNode if n.value.length < cutoff => n.value
        case n: TreeNode => n.value.take(cutoff) + "..."
        case x => x.nodeName
      })

    def info(f: GraphNode) = {
      val significance = f match {
        case tn: TreeNode =>
          tn.significance match {
            case Noise => "_"
            case Signal => "!"
            case Undeclared => "."
          }
        case _ => ""
      }

      s"${significance} ${f.nodeName} (${f.getClass.getSimpleName}#${f.hashCode()}) ${offset(f)}:[${showValue(f, 50)}]"
    }

    def toShortStr(fv: GraphNode, depth: Int, shown: GraphNode => Boolean): String = fv match {
      case ctn if ctn.relatedNodes.nonEmpty =>
        def star(c: GraphNode) = if (shown(c)) "*" else ""

        tabs(depth) + info(ctn) + (if (ctn.relatedNodes.nonEmpty) ":\n" else "") + ctn.relatedNodes.map(c => star(c) + toShortStr(c, depth + 1, shown)).mkString("\n")
      case f => tabs(depth) + info(f) + "\n"
    }

    toShortStr(tn, 0, _ => true)
  }

  /**
    * Shows name tags of all nodes
    */
  val NameAndTagsStringifier: TreeNode => String =
    tn => s"${tn.nodeName}:[${tn.nodeTags.mkString(",")}]"

  /**
    * Shows content of terminal nodes: Tags otherwise
    */
  val NameAndContentStringifier: TreeNode => String = {
    case tn if tn.childNodes.isEmpty =>
      s"${tn.nodeName}:[${tn.value}]"
    case tn => s"${tn.nodeName}:[${tn.nodeTags.mkString(", ")}]"
  }

  private def toShorterStringInternal(tn: TreeNode, nodeStringifier: TreeNode => String): Seq[String]

  = {
    val shorterString = nodeStringifier(tn)
    val lines = shorterString +:
      tn.childNodes.flatMap(tn => toShorterStringInternal(tn, nodeStringifier))
    lines.map("  " + _)
  }

  /**
    * Return a string showing the structure of the tree but not the content
    */
  def toShorterString(tn: TreeNode, nodeStringifier: TreeNode => String = NameAndTagsStringifier): String =
    toShorterStringInternal(tn, nodeStringifier).mkString("\n")

}
