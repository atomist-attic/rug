package com.atomist.tree.content.text.microgrammar.matchers

import com.atomist.tree.TreeNode
import com.atomist.tree.content.text.MutableTerminalTreeNode
import com.atomist.tree.content.text.microgrammar.Matcher.MatchPrefixResult
import com.atomist.tree.content.text.microgrammar.{DismatchReport, InputState, Matcher, PatternMatch}
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

  override def shortDescription(knownMatchers: Map[String, Matcher]): String = s"...${breakToMatcher.shortDescription(knownMatchers)}"

  override def matchPrefixInternal(inputState: InputState): MatchPrefixResult =
    if (!inputState.exhausted) {
      var currentInputState = inputState
      var matchedTerminatingPattern = breakToMatcher.matchPrefix(currentInputState)
      while (matchedTerminatingPattern.isLeft && !currentInputState.exhausted) {
        // Advance one character
        currentInputState = currentInputState.advance
        matchedTerminatingPattern = breakToMatcher.matchPrefix(currentInputState)
      }
      // We either exhausted the input and didn't match at all, or we have a match
      matchedTerminatingPattern match {
        case Left(no) =>
          Left(no.andSo("not anywhere in the rest of the input"))
        case Right(terminatingMatch) =>
          val (eaten, _) = inputState.take(terminatingMatch.endPosition.offset - inputState.offset)
          logger.debug(s"terminatingMatch=[$terminatingMatch],eaten=[$eaten]")
          Right(terminatingMatch)
      }
    }
    else
      Left(DismatchReport("no input left"))
}
