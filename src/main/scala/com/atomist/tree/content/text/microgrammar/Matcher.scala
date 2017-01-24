package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.MutableTreeNode
import com.atomist.tree.content.text._
import com.atomist.tree.content.text.microgrammar.PatternMatch.MatchedNode

case class MatcherConfig(
                          greedy: Boolean = true
                        )

/**
  * Extended by classes that can match part of an input string, preserving
  * information and position. Patterns are composable in SNOBOL style.
  */
trait Matcher {

  def name: String

  /**
    * Match this string at the present offset. Return None
    * in event of failure
    *
    * @param inputState input state
    * @return match or failure to match
    */
  protected def matchPrefixInternal(inputState: InputState): Option[PatternMatch]

  final def matchPrefix(is: InputState) = {
    val matchedOption = matchPrefixInternal(is)

    for {matched <- matchedOption
         node <- matched.node} {
        node.addType(name)
    }
    matchedOption
  }

  def concat(m: Matcher): Matcher = Concat(this, m)

  def ~(m: Matcher): Matcher = concat(m)

  /**
    * Concatenation requiring whitespace
    *
    * @param m
    * @return
    */
  def ~~(m: Matcher): Matcher = concat(WhitespaceOrNewLine.concat(m))

  /**
    * Concatenation with optional whitespace
    *
    * @param m
    * @return
    */
  def ~?(m: Matcher): Matcher = concat(Whitespace.?().concat(m))

  def alternate(m: Matcher): Matcher = Alternate(this, m)

  def ?(): Matcher = opt

  def opt: Matcher = Optional(this)

  def |(m: Matcher): Matcher = alternate(m)

  def -(): Matcher = Discard(this)

}


/**
  * Trait extended by Matchers that are configurable.
  */
trait ConfigurableMatcher extends Matcher {

  def config: MatcherConfig
}

object PatternMatch {

  type MatchedNode = PositionedTreeNode with MutableTreeNode
}

/**
  * Returned for a pattern match.
  *
  * @param node    matched node. If None, the match is discarded.
  * @param matched the string that was matched
  * @param resultingInputState   resulting InputState
  */
case class PatternMatch(
                         node: Option[MatchedNode],
                         matched: String,
                         resultingInputState: InputState,
                         matcherId: String)
  extends Positioned {

  def remainderOffset: Int = resultingInputState.offset + matched.length

  override def startPosition: InputPosition = endPosition - matched.length

  override def endPosition: InputPosition = resultingInputState.inputPosition

  def remainder: CharSequence = resultingInputState.input.subSequence(remainderOffset, resultingInputState.input.length())

}
