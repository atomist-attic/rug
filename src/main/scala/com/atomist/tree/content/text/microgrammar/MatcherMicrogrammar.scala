package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.{ContainerTreeNode, TreeNode}
import com.atomist.tree.content.text.grammar.MatchListener
import com.atomist.tree.content.text.microgrammar.PatternMatch.MatchedNode
import com.atomist.tree.content.text.{MutableContainerTreeNode, SimpleMutableContainerTreeNode}

import scala.collection.mutable.ListBuffer

/**
  * Uses our PatternMatch mechanism for SNOBOL-style composable pattern matching
  */
class MatcherMicrogrammar(matcher: Matcher) extends Microgrammar {

  // TODO input should be a CharSequence, not a string

  override def findMatches(input: String, l: Option[MatchListener]): Seq[MutableContainerTreeNode] = {
    val nodes = findMatchesInternal(input, l)
    nodes collect {
      case mut: MutableContainerTreeNode => mut
    }
  }

  override def strictMatch(input: String, l: Option[MatchListener]): MutableContainerTreeNode = {
    val nodes = findMatchesInternal(input, l)
    require(nodes.size == 1, s"Expected 1 result, not ${nodes.size}")
    nodes.head match {
      case mctn: MutableContainerTreeNode => mctn
      case tn: TreeNode => SimpleMutableContainerTreeNode.wholeInput("input", Seq(tn), input)
    }
  }

  private def findMatchesInternal(input: String, l: Option[MatchListener]): Seq[MatchedNode] = {
    var offset = 0
    val nodes = ListBuffer.empty[PatternMatch.MatchedNode]
    while (offset < input.length) {
      //println(s"Offset is $offset in [$input] of length ${input.length}")
      matcher.matchPrefix(offset, input) match {
        case None =>
          offset += 1
        case Some(m) =>
          println(s"Found match $m, remainderOffset=${m.remainderOffset}")
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