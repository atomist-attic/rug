package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.TreeNode
import com.atomist.tree.content.text.TreeNodeOperations._
import com.atomist.tree.content.text._
import com.atomist.tree.content.text.grammar.MatchListener

import scala.collection.mutable.ListBuffer

/**
  * Uses our PatternMatch mechanism for SNOBOL-style composable pattern matching
  */
class MatcherMicrogrammar(val matcher: Matcher, val name: String = "MySpecialMicrogrammar") extends Microgrammar {

  // Transformation to run on matched nodes
  private val transform = collapse(
    ctn => ctn.significance == TreeNode.Structural
    , "it's a concat") andThen RemovePadding andThen RemoveStructuralLiterals andThen Prune

  override def findMatches(input: CharSequence, l: Option[MatchListener]): Seq[MutableContainerTreeNode] = {
    val (matches, dismatches) = findMatchesInternal(input, l)
    val processedNodes = matches.map { case (m, o) =>
      outputNode(input)(m, o)
    }
    processedNodes
  }

  def strictMatch(input: CharSequence, l: Option[MatchListener] = None): Either[DismatchReport, MutableContainerTreeNode] = {
    val result = matcher.matchPrefix(InputState(input))
    result.right.map(matched => outputNode(input)(matched, OffsetInputPosition(0)))
  }

  private [microgrammar] def outputNode(input: CharSequence)(matchFound: PatternMatch, startOffset: InputPosition = OffsetInputPosition(0)) = {
    val endOffset = startOffset + matchFound.matched.length
    val matchedNode = matchFound.node match {
      case None =>
        new MicrogrammarNode(name, name, Seq(), startOffset, endOffset)
      case Some(one: MutableTerminalTreeNode) =>
        new MicrogrammarNode(name, name, Seq(one), startOffset, endOffset)
      case Some(container: MutableContainerTreeNode) =>
        new MicrogrammarNode(name, name, container.childNodes, startOffset, endOffset)
    }
    matchedNode.pad(input.toString)
    transform(matchedNode)
  }


  private[microgrammar] def findMatchesInternal(input: CharSequence,
                                                listeners: Option[MatchListener]): (Seq[(PatternMatch, OffsetInputPosition)], Seq[DismatchReport]) = {
    val matches = ListBuffer.empty[(PatternMatch, OffsetInputPosition)]
    val dismatches = ListBuffer.empty[DismatchReport]
    var is = InputState(input)
    while (!is.exhausted) {
      matcher.matchPrefix(is) match {
        case Left(dismatchReport) =>
          dismatches.append(dismatchReport)
          is = is.advance
        case Right(matchFound) =>
          listeners.foreach(l => matchFound.node.map(l.onMatch(_)))
          val thisStartedAt = OffsetInputPosition(is.offset)
          matches.append(matchFound -> thisStartedAt)
          is = matchFound.resultingInputState
      }
    }

    (matches, dismatches)
  }

  override def toString: String = s"MatcherMicrogrammar wrapping [$matcher]"

}

private class MicrogrammarNode(name: String,
                                typ: String,
                                fields: Seq[TreeNode],
                                startPosition: InputPosition,
                                endPosition: InputPosition)
  extends SimpleMutableContainerTreeNode(
    name: String, fields, startPosition, endPosition, significance = TreeNode.Explicit) {

  addType(typ)
  addType(MicrogrammarNode.MicrogrammarNodeType)
}

object MicrogrammarNode {

  /**
    * Node type added for all microgrammar nodes
    */
  val MicrogrammarNodeType = "microgrammar"
}