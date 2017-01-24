package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.content.text.TreeNodeOperations._
import com.atomist.tree.content.text.grammar.MatchListener
import com.atomist.tree.content.text._
import com.atomist.tree.content.text.microgrammar.PatternMatch.MatchedNode
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
    val rawNodes = findMatchesInternal(input, l)
    val processedNodes = rawNodes collect {
      case mut: MutableContainerTreeNode =>
        outputNode(input, mut)
      case tn: TerminalTreeNode =>
        val raw = new MicrogrammarNode(tn.nodeName, tn.nodeName, Seq(tn), tn.startPosition, tn.endPosition)
        SimpleMutableContainerTreeNode.wrap(name, raw)
    }
    // Add the microgrammar type annotation to every top level node we return
    processedNodes.foreach(n => n.addType(MicrogrammarNode.MicrogrammarNodeType))
    processedNodes.foreach {
      case amut: AbstractMutableContainerTreeNode =>
        amut.pad(input.toString, padAtBeginning = true)
      case _ =>
    }
    rawNodes.zip(processedNodes).foreach {
      case (raw, cooked) => require(raw.value == cooked.value)
    }

    processedNodes
  }

  private def outputNode(input: CharSequence, n: TreeNode) = {
    //println(s"Before transform, node=\n${TreeNodeUtils.toShortString(n)}")
    n match {
      case mctn: AbstractMutableContainerTreeNode =>
        mctn.pad(input, padAtBeginning = true)
        transform(mctn)
      case mctn: MutableContainerTreeNode =>
        transform(mctn)
      case tn: TreeNode =>
        val ret = SimpleMutableContainerTreeNode.wholeInput("input", Seq(tn), input.toString)
        require(ret.padded)
        ret
    }
  }

  override def strictMatch(input: CharSequence, l: Option[MatchListener]): MutableContainerTreeNode = {
    val nodes = findMatchesInternal(input, l)
    require(nodes.size == 1, s"Expected 1 result, not ${nodes.size}")
    outputNode(input, nodes.head)
  }

  private def findMatchesInternal(input: CharSequence, listeners: Option[MatchListener]) = {
    val nodes = ListBuffer.empty[PatternMatch.MatchedNode]
    var is = InputState(input)
    while (!is.exhausted) {
      val thisStartedAt = LineHoldingOffsetInputPosition(input, is.offset)
      matcher.matchPrefix(is) match {
        case None =>
          is = is.advance
        case Some(matchFound) =>
          listeners.foreach(l => matchFound.node collect {
            case ctn: ContainerTreeNode => l.onMatch(ctn)
          })
        val matchedNode = matchFound.node match {
           case None =>
             new MutableTerminalTreeNode("matcher name goes here", matchFound.matched, thisStartedAt)
           case Some(one: MutableTerminalTreeNode) =>
             new SimpleMutableContainerTreeNode("matcher name goes here", Seq(one), thisStartedAt, LineHoldingOffsetInputPosition(input, is.offset + matchFound.matched.length))
           case Some(container: MutableContainerTreeNode) =>
             new SimpleMutableContainerTreeNode("matcher name goes here", container.childNodes.map(_.asInstanceOf[MatchedNode]), thisStartedAt, LineHoldingOffsetInputPosition(input, is.offset + matchFound.matched.length))
         }
          matchedNode.pad(input, padAtBeginning = true)
          matchFound.node.foreach(n => nodes.append(n))
          is = matchFound.resultingInputState
      }
    }
    nodes
  }

  override def toString: String = s"MatcherMicrogrammar wrapping [$matcher]"

}

private class MicrogrammarNode(name: String,
                                typ: String,
                                fields: Seq[TreeNode],
                                startPosition: InputPosition,
                                endPosition: InputPosition)
  extends SimpleMutableContainerTreeNode(
    name: String, fields, startPosition, endPosition) {

  addType(typ)
  addType(MicrogrammarNode.MicrogrammarNodeType)
}

object MicrogrammarNode {

  /**
    * Node type added for all microgrammar nodes
    */
  val MicrogrammarNodeType = "microgrammar"
}