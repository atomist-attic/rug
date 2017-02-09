package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.TreeNode
import com.atomist.tree.content.text.microgrammar.Matcher.MatchPrefixResult
import com.atomist.tree.content.text.{MutableTerminalTreeNode, OffsetInputPosition, SimpleMutableContainerTreeNode}

/**
  * Matches a literal string
  *
  * @param literal literal string to match
  * @param named   name (optional) if we are returning a node
  */
case class Literal(literal: String, named: Option[String] = None) extends Matcher {
  import Literal._

  override def name: String = named.getOrElse(LiteralDefaultName)

  override def matchPrefixInternal(inputState: InputState): MatchPrefixResult = {
    val (matched, is) = inputState.take(literal.length)
    if (matched == literal) {
      val node = named match {
        case Some(name) =>
          val node = new MutableTerminalTreeNode(name, literal, inputState.inputPosition)
          node.addType(name)
          node
        case None =>
          new MutableTerminalTreeNode(LiteralDefaultName, literal, inputState.inputPosition, significance = TreeNode.Noise)
      }
      Right(PatternMatch(
        node,
        literal,
        is,
        this.toString))
    }
    else {
      Left(DismatchReport(s"Literal: <$literal> != <$matched>").at(OffsetInputPosition(inputState.offset), OffsetInputPosition(inputState.offset + literal.length)))
    }
  }

}

object Literal {

  val LiteralDefaultName = ".literal"

  implicit def stringToMatcher(s: String): Matcher = Literal(s)
}

/**
  * Remainder of the the current line
  */
case class RestOfLine(name: String = "restOfLine") extends Matcher {

  override def matchPrefixInternal(inputState: InputState): MatchPrefixResult =
    ???

  // Some(PatternMatch(null, offset, s.toString, ""))
}

/**
  * Reference to another matcher.
  */
case class Reference(name: String) extends Matcher {

  override def matchPrefixInternal(inputState: InputState): MatchPrefixResult = {
    val matcherOpt = inputState.knownMatchers.get(name)
    matcherOpt match {
      case Some(matcher) =>
        matcher.matchPrefix(inputState).right.map{ matched =>
          val wrappedNode =
            SimpleMutableContainerTreeNode.wrap(name, matched.node)
          matched.copy(node = wrappedNode)
        }
      case _ =>
        throw new IllegalStateException(s"Could not find matcher '$name'.")
    }

  }
}