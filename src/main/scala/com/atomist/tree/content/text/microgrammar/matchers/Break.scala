package com.atomist.tree.content.text.microgrammar.matchers

import com.atomist.tree.content.text.microgrammar.{Matcher, PatternMatch}
import com.atomist.tree.content.text.{MutableTerminalTreeNode, OffsetInputPosition}

/**
  * Similar to a SNOBOL "break". If we don't eventually find the literal,
  * we don't match. Matches the content up to and including the final matcher.
  *
  * @param breakToMatcher matcher that might match
  * @param named          Name if we have one. If we don't,
  *                       no node will be created, which effectively discards the content
  */
case class Break(breakToMatcher: Matcher, named: Option[String] = None) extends Matcher {

  override def name: String = named.getOrElse("break")

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
          val eaten = take(offset, input, m.endPosition.offset - offset)
          Some(PatternMatch(
            named.map(n =>
              new MutableTerminalTreeNode(n, eaten, OffsetInputPosition(offset))),
            offset,
            eaten,
            input,
            this.toString))
      }
    }
    else
      None
}
