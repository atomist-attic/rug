package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.content.text.{MutableTerminalTreeNode, OffsetInputPosition}

/**
  * Matches a literal string
  *
  * @param literal literal string to match
  * @param named   name (optional) if we are returning a node
  */
case class Literal(literal: String, named: Option[String] = None) extends Matcher {

  override def name = named.getOrElse("literal")

  override def matchPrefix(offset: Int, input: CharSequence): Option[PatternMatch] =
    if (input != null && input.length() > 0 && input.length() >= offset + literal.length) {
      val possibleMatch = take(offset, input, literal.length)
      if (possibleMatch.equals(literal))
        Some(PatternMatch(
          named.map(name => new MutableTerminalTreeNode(name, literal, OffsetInputPosition(offset))),
          offset, literal, input, this.toString))
      else
        None
    }
    else
      None

}

object Literal {

  implicit def stringToMatcher(s: String): Matcher = Literal(s)
}

/**
  * Remainder of the the input
  */
case class Remainder(name: String = "remainder") extends Matcher {

  override def matchPrefix(offset: Int, input: CharSequence): Option[PatternMatch] =
    Some(
      PatternMatch(node = null, offset = offset,
        matched = take(offset, input, input.length() - offset),
        input = input, this.toString)
    )

}

/**
  * Remainder of the the current line
  */
case class RestOfLine(name: String = "restOfLine") extends Matcher {

  override def matchPrefix(offset: Int, s: CharSequence): Option[PatternMatch] =
    ???

  // Some(PatternMatch(null, offset, s.toString, ""))
}

/**
  * Reference to another matcher.
  */
case class Reference(delegate: Matcher, name: String) extends Matcher {

  override def matchPrefix(offset: Int, input: CharSequence): Option[PatternMatch] =
    delegate.matchPrefix(offset, input).map(m => m.copy(node = ???))
}