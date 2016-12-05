package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.content.text.grammar.MatchListener
import com.atomist.tree.content.text.{MutableContainerTreeNode, SimpleMutableContainerTreeNode}

import scala.collection.mutable.ListBuffer

/**
  * Uses our PatternMatch mechanism for SNOBOL-style composable pattern matching
  */
class MatcherMicrogrammar(matcher: Matcher) extends Microgrammar {

  // TODO input should be a CharSequence, not a string

  /**
    * Match all input, which must exactly match input.
    */
  override def findMatches(input: String, l: Option[MatchListener]): Seq[MutableContainerTreeNode] = {
    var offset = 0
    val nodes = ListBuffer.empty[PatternMatch.MatchedNode]
    while (offset < input.length) {
      println(s"Offset is $offset in [$input] of length ${input.length}")
      matcher.matchPrefix(offset, input) match {
        case None =>
          offset += 1
        case Some(m) =>
          println(s"Found match $m, remainderOffset=${m.remainderOffset}")
          m.node.map(n => nodes.append(n))
          offset = m.remainderOffset
      }
    }
    Seq(SimpleMutableContainerTreeNode.wholeInput("input", nodes, input))
  }

  override def strictMatch(input: String, l: Option[MatchListener]): MutableContainerTreeNode = {
    val results = findMatches(input, l)
    require(results.size == 1, s"Expected 1 result, not ${results.size}")
    val r = results.head
    r
  }
}