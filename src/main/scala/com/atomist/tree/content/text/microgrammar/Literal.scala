package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.TreeNode
import com.atomist.tree.content.text.microgrammar.Matcher.MatchPrefixResult
import com.atomist.tree.content.text.{MutableTerminalTreeNode, OffsetInputPosition}

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
          new MutableTerminalTreeNode(LiteralDefaultName, literal, inputState.inputPosition, significance = TreeNode.Structural)
      }
      Right(PatternMatch(
        Some(node),
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
  * Remainder of the the input
  */
case class Remainder(name: String = "remainder") extends Matcher {

  override def matchPrefixInternal(inputState: InputState): MatchPrefixResult = {
    if (inputState.exhausted)
      Left(DismatchReport("There is nothing here")) // Why would this stop a match?
    else {
      val (matched, is) = inputState.takeAll
      Right(
        PatternMatch(None,
          matched,
          is, this.toString)
      )
    }
  }

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
case class Reference(delegate: Matcher, name: String) extends Matcher {

  override def matchPrefixInternal(inputState: InputState): MatchPrefixResult =
    delegate.matchPrefix(inputState).right.map(m => m.copy(node = ???))
}