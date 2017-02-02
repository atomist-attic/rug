package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.{ContainerTreeNode, TerminalTreeNode, TreeNode}
import com.atomist.tree.content.text.{OffsetInputPosition, PositionedMutableContainerTreeNode, PositionedTreeNode}

case class DismatchReport(why: String,
                           causes: Seq[DismatchReport] = Seq(),
                           priorMatch: Option[PatternMatch] = None,
                           startOffset: Option[OffsetInputPosition] = None,
                           endOffset: Option[OffsetInputPosition] = None) {

  def at(startOffset: OffsetInputPosition, endOffset: OffsetInputPosition) = {
    copy(startOffset = Some(startOffset), endOffset = Some(endOffset))
  }

  def lengthOfClosestMatch: Int = {
    val priorMatchLen = priorMatch.map(_.node.value.length)
    (Seq(0) ++ causes.map(_.lengthOfClosestMatch) ++ priorMatchLen).max
  }

  def andSo(consequence: String): DismatchReport = DismatchReport(consequence, Seq(this), startOffset = startOffset, endOffset = endOffset)

  def withPriorMatch(p: PatternMatch) = copy(priorMatch = Some(p))
}

object DismatchReport {

  def detailedReport(dr: DismatchReport, input: String): String = {
    detailedReportLines(dr, input).mkString("\n")
  }

  private def detailedReportLines(dr: DismatchReport, input: String): Seq[String] = {
    val display =
      (for {start <- dr.startOffset
            end <- dr.endOffset}
        yield {
          insertCharacter("{", start.offset, insertCharacter("}", end.offset, input))
        }).getOrElse(input)

    val whatDidMatch =
      dr.priorMatch.map(pm => printMatching(pm.node, display))
    val lines = whatDidMatch.toSeq ++ Seq(dr.why) ++ dr.causes.flatMap(detailedReportLines(_, display))

    lines.map(" " + _)
  }

  def insertCharacter(what: String, where: Int, in: String): String =
    in.substring(0, where) + what + in.substring(where)

  def printMatching(node: PositionedTreeNode, input: String): String = {
    node match {
      case n: PositionedMutableContainerTreeNode => n.pad(input)
      case _ =>
    }
    val startOffset = node.startPosition.offset
    val endOffset = node.endPosition.offset

    val markChildren: String => String =
      node match {
        case tn: TerminalTreeNode => identity
        case ctn: ContainerTreeNode =>
          val childrenByStartOffset = ctn.childNodes.map(_.asInstanceOf[PositionedTreeNode]).sortBy(_.startPosition.offset)
          val pf: String => String = {
            case input =>
              childrenByStartOffset.reverse.foldLeft(input) { (inp: String, child: PositionedTreeNode) =>
                printMatching(child, inp)
              }
          }
          pf
      }

    val name = if(node.significance == TreeNode.Signal)
      s"${node.nodeName}="
    else
       ""
    insertCharacter(s"[$name", startOffset,
      markChildren(
        insertCharacter("]", endOffset, input)))

  }
}