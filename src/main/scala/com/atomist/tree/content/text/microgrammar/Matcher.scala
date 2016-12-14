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
    * @param offset offset within a whole input that we're matching at.
    *               We keep this rather than the whole input as a string as a potential
    *               efficiency measure
    * @param input  whole input we're matching
    * @return match or failure to match
    */
  def matchPrefix(offset: Int, input: CharSequence): Option[PatternMatch]

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
  def ~?(m: Matcher): Matcher = concat(Whitespace.?.concat(m))

  def alternate(m: Matcher): Matcher = Alternate(this, m)

  def ?(): Matcher = opt

  def opt: Matcher = Optional(this)

  def |(m: Matcher): Matcher = alternate(m)

  def -(): Matcher = Discard(this)

  /**
    * Utility method to take next characters from input
    *
    * @param offset start offset
    * @param input  input sequence
    * @param n      number of characters to take
    * @return a String
    */
  protected def take(offset: Int, input: CharSequence, n: Int): String = {
    input.subSequence(offset, offset + n).toString
  }
}

trait TerminalMatcher extends Matcher {

  def name: String

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
  * @param offset  offset in the input at which the match begins
  * @param matched the string that was matched
  * @param input   the entire input
  */
case class PatternMatch(
                         node: Option[MatchedNode],
                         offset: Int,
                         matched: String,
                         input: CharSequence,
                         matcherId: String)
  extends Positioned {

  def remainderOffset: Int = offset + matched.length

  override def startPosition: InputPosition = OffsetInputPosition(offset)

  override def endPosition: InputPosition = OffsetInputPosition(offset + matched.length)

  def remainder: CharSequence = input.subSequence(remainderOffset, input.length())

}
