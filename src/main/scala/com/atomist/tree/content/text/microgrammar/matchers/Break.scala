package com.atomist.tree.content.text.microgrammar.matchers

import com.atomist.tree.content.text.microgrammar.{InputState, Matcher, PatternMatch}
import com.atomist.tree.content.text.{MutableTerminalTreeNode, OffsetInputPosition}

/**
  * Similar to a SNOBOL "break". If we don't eventually find the terminating pattern in breakToMatcher,
  * we don't match. Matches the content up to and including the final matcher.
  *
  * @param breakToMatcher matcher that might match
  * @param named          Name if we have one. If we don't,
  *                       no node will be created, which effectively discards the content
  */
case class Break(breakToMatcher: Matcher, named: Option[String] = None) extends Matcher {

  override def name: String = named.getOrElse("break")

  override def matchPrefix(inputState: InputState): Option[PatternMatch] =
    if (!inputState.exhausted) {
      var is = inputState
      var matchedTerminatingPattern = breakToMatcher.matchPrefix(is)
      while (matchedTerminatingPattern.isEmpty && !is.exhausted) {
        // Advance one character
        is = is.advance
        matchedTerminatingPattern = breakToMatcher.matchPrefix(is)
      }
      matchedTerminatingPattern match {
        case None =>
          None
        case Some(m) =>
          val (eaten,resultingIs) = inputState.take(m.endPosition.offset - inputState.offset - 1)
          Some(PatternMatch(
            named.map(n =>
              new MutableTerminalTreeNode(n, eaten, inputState.inputPosition)),
            eaten,
            resultingIs,
            this.toString))
      }
    }
    else
      None
}
