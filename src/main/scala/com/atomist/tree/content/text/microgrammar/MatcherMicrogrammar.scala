package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.{ContainerTreeNode, TreeNode}
import com.atomist.tree.content.text.grammar.MatchListener
import com.atomist.tree.content.text.microgrammar.PatternMatch.MatchedNode
import com.atomist.tree.content.text.{AbstractMutableContainerTreeNode, MutableContainerTreeNode, SimpleMutableContainerTreeNode}

import scala.collection.mutable.ListBuffer

import com.atomist.tree.content.text.TreeNodeOperations._

/**
  * Uses our PatternMatch mechanism for SNOBOL-style composable pattern matching
  */
class MatcherMicrogrammar(matcher: Matcher) extends Microgrammar {

  // TODO input should be a CharSequence, not a string

  private val transform = collapse(ctn => ctn.nodeName.equals("literal")) andThen Clean

  override def findMatches(input: String, l: Option[MatchListener]): Seq[MutableContainerTreeNode] = {
    val nodes = findMatchesInternal(input, l)
    nodes collect {
      case mut: MutableContainerTreeNode =>
        outputNode(input, mut)
    }
  }

  private def outputNode(input: String, n: TreeNode) = n match {
    case mctn: AbstractMutableContainerTreeNode =>
      mctn.pad(input)
      transform(mctn)
    case mctn: MutableContainerTreeNode =>
      transform(mctn)
    case tn: TreeNode =>
      SimpleMutableContainerTreeNode.wholeInput("input", Seq(tn), input)
  }

  override def strictMatch(input: String, l: Option[MatchListener]): MutableContainerTreeNode = {
    val nodes = findMatchesInternal(input, l)
    require(nodes.size == 1, s"Expected 1 result, not ${nodes.size}")
    outputNode(input, nodes.head)
  }

  private def findMatchesInternal(input: String, l: Option[MatchListener]) = {
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
}