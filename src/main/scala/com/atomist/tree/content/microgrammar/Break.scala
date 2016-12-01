package com.atomist.tree.content.microgrammar

import com.atomist.tree.content.text.{MutableTerminalTreeNode, OffsetInputPosition}

/**
  * Similar to a SNOBOL break. If we don't eventually find the literal,
  * we don't match.
  *
  * @param breakToMatcher matcher that might match
  */
case class Break(breakToMatcher: Matcher, named: Option[String] = None) extends Matcher {

  override def name = named.getOrElse("break")

  override def matchPrefix(offset: Int, input: CharSequence): Option[PatternMatch] =
    if (input != null && input.length() > 0) {
      var last = offset
      var matched = breakToMatcher.matchPrefix(last, input)
      while (matched.isEmpty && last < input.length()) {
        // Advance one character
        last += 1
        matched = breakToMatcher.matchPrefix(last, input)
      }
      matched match {
        case None =>
          None
        case Some(m) =>
          val eaten = take(offset, input, last - offset)
          Some(PatternMatch(
            named.map(n => new MutableTerminalTreeNode(n, eaten, OffsetInputPosition(offset))),
            offset,
            eaten, input, this.toString))
      }
    }
    else
      None
}
