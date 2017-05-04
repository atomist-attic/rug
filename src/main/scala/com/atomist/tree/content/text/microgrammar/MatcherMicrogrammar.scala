package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.content.text._
import com.atomist.tree.content.text.grammar.MatchListener
import com.atomist.tree.{ContainerTreeNode, TerminalTreeNode, TreeNode}

import scala.collection.mutable.ListBuffer

/**
  * Uses our PatternMatch mechanism for SNOBOL-style composable pattern matching
  */
class MatcherMicrogrammar(val matcher: Matcher,
                          val name: String = "MySpecialMicrogrammar",
                          submatchers: Map[String, Matcher] = Map()) extends Microgrammar {

  def shortDescription = matcher.shortDescription(submatchers)

  override def findMatches(input: CharSequence, l: Option[MatchListener]): Seq[PositionedTreeNode] = {
    val (matches, dismatches) = findMatchesInternal(input, l)
    val processedNodes = matches.map(outputNode(input))
    //println(DismatchReport.detailedReport(dismatches.maxBy(_.lengthOfClosestMatch), input.toString))
    processedNodes
  }

  def strictMatch(input: CharSequence, l: Option[MatchListener] = None): Either[DismatchReport, PositionedTreeNode] = {
    matcher.matchPrefix(InputState(input, knownMatchers = submatchers)).right.map(outputNode(input))
  }

  /* create the match node, named after the microgrammar */
  private[microgrammar] def outputNode(input: CharSequence)(matchFound: PatternMatch): PositionedTreeNode = {
    ImmutablePositionedTreeNode(matchFound.node)
    val endOffset = matchFound.node.endPosition
    val startOffset = matchFound.node.startPosition
    val children = matchFound.node match {
      case one: TerminalTreeNode =>
        Seq(one)
      case container: ContainerTreeNode =>
        container.childNodes.map(_.asInstanceOf[PositionedTreeNode])
      case other => throw new IllegalArgumentException(s"What kind of node is not Terminal or Container? An $other node! Boo!")
    }
    new ImmutablePositionedTreeNode(name, startOffset, endOffset, children, Set(), TreeNode.Signal)
  }

  private[microgrammar] def findMatchesInternal(input: CharSequence,
                                                listeners: Option[MatchListener]): (Seq[PatternMatch], Seq[DismatchReport]) = {
    val matches = ListBuffer.empty[PatternMatch]
    val dismatches = ListBuffer.empty[DismatchReport]
    var is = InputState(input, knownMatchers = submatchers)
    while (!is.exhausted) {
      matcher.matchPrefix(is) match {
        case Left(dismatchReport) =>
          dismatches.append(dismatchReport)
          is = is.advance
        case Right(matchFound) =>
          listeners.foreach(l => l.onMatch(matchFound.node))
          matches.append(matchFound)
          is = matchFound.resultingInputState
      }
    }

    (matches, dismatches)
  }

  override def toString: String = s"MatcherMicrogrammar wrapping [$matcher]"

}