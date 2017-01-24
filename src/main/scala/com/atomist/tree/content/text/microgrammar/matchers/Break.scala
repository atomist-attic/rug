package com.atomist.tree.content.text.microgrammar.matchers

import com.atomist.tree.content.text.MutableTerminalTreeNode
import com.atomist.tree.content.text.microgrammar.{InputState, Matcher, PatternMatch}
import com.typesafe.scalalogging.LazyLogging

/**
  * Similar to a SNOBOL "break". If we don't eventually find the terminating pattern in breakToMatcher,
  * we don't match. Matches the content up to and including the final matcher.
  *
  * @param breakToMatcher matcher that might match
  * @param named          Name if we have one. If we don't,
  *                       no node will be created, which effectively discards the content
  */
case class Break(breakToMatcher: Matcher, named: Option[String] = None)
  extends Matcher with LazyLogging {

  override def name: String = named.getOrElse("break")

  override def matchPrefixInternal(inputState: InputState): Option[PatternMatch] =
    if (!inputState.exhausted) {
      var currentInputState = inputState
      var matchedTerminatingPattern = breakToMatcher.matchPrefix(currentInputState)
      while (matchedTerminatingPattern.isEmpty && !currentInputState.exhausted) {
        // Advance one character
        currentInputState = currentInputState.advance
        matchedTerminatingPattern = breakToMatcher.matchPrefix(currentInputState)
      }
      // We either exhausted the input and didn't match at all, or we have a match
      matchedTerminatingPattern match {
        case None =>
          None
        case Some(terminatingMatch) =>
          val (eaten, resultingIs) = inputState.take(terminatingMatch.endPosition.offset - inputState.offset)
          val returnedMatch = PatternMatch(
            named.map(n =>
              new MutableTerminalTreeNode(n, eaten, inputState.inputPosition)),
            eaten,
            terminatingMatch.resultingInputState,
            this.toString)
          logger.debug(s"terminatingMatch=[$terminatingMatch],eaten=[$eaten], matched=$returnedMatch")
          Some(returnedMatch)
      }
    }
    else
      None
}
