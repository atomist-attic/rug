package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.content.text.TreeNodeOperations._
import com.atomist.tree.content.text.grammar.MatchListener
import com.atomist.tree.content.text._
import com.atomist.tree.{ContainerTreeNode, TerminalTreeNode, TreeNode}

import scala.collection.mutable.ListBuffer

/**
  * Uses our PatternMatch mechanism for SNOBOL-style composable pattern matching
  */
class MatcherMicrogrammar(val matcher: Matcher) extends Microgrammar {

  // Transformation to run on matched nodes
  private val transform = collapse(
    ctn => ctn.nodeName.equals(Concat.DefaultConcatName)
  ) andThen RemovePadding andThen Prune

  override def name: String = matcher.name

  override def findMatches(input: CharSequence, l: Option[MatchListener]): Seq[MutableContainerTreeNode] = {
    val nodes = findMatchesInternal(input, l)
    nodes collect {
      case mut: MutableContainerTreeNode =>
        outputNode(input, mut)
      case tn: TerminalTreeNode =>
        val raw = new MicrogrammarField(tn.nodeName, tn.nodeName, Seq(tn), tn.startPosition, tn.endPosition)
        SimpleMutableContainerTreeNode.wrap(name, raw)
    }
  }

  private def outputNode(input: CharSequence, n: TreeNode) = {
    //println(s"Before transform, node=\n${TreeNodeUtils.toShortString(n)}")
    n match {
      case mctn: AbstractMutableContainerTreeNode =>
        mctn.pad(input.toString)
        transform(mctn)
      case mctn: MutableContainerTreeNode =>
        transform(mctn)
      case tn: TreeNode =>
        SimpleMutableContainerTreeNode.wholeInput("input", Seq(tn), input.toString)
    }
  }

  override def strictMatch(input: CharSequence, l: Option[MatchListener]): MutableContainerTreeNode = {
    val nodes = findMatchesInternal(input, l)
    require(nodes.size == 1, s"Expected 1 result, not ${nodes.size}")
    outputNode(input, nodes.head)
  }

  private def findMatchesInternal(input: CharSequence, l: Option[MatchListener]) = {
    var offset = 0
    val nodes = ListBuffer.empty[PatternMatch.MatchedNode]
    while (offset < input.length) {
      matcher.matchPrefix(offset, input) match {
        case None =>
          offset += 1
        case Some(m) =>
          l.foreach(l => m.node collect {
            case ctn: ContainerTreeNode => l.onMatch(ctn)
          })
          m.node.foreach(n => nodes.append(n))
          offset = m.remainderOffset
      }
    }
    nodes
  }

  override def toString: String = s"MatcherMicrogrammar wrapping [$matcher]"

}

private class MicrogrammarField(name: String,
                                typ: String,
                                fields: Seq[TreeNode],
                                startPosition: InputPosition,
                                endPosition: InputPosition)
  extends SimpleMutableContainerTreeNode(
    name: String, fields, startPosition, endPosition) {

  addType(typ)
}