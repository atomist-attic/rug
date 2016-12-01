package com.atomist.model.content.grammar.microgrammar.pattern

import com.atomist.model.content.grammar.microgrammar.pattern.PatternMatch.MatchedNode
import com.atomist.model.content.text._

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

  def ~(m: Matcher) = concat(m)

  /**
    * Concatenation requiring whitespace
    *
    * @param m
    * @return
    */
  def ~~(m: Matcher) = concat(WhitespaceOrNewLine.concat(m))

  /**
    * Concatenation with optional whitespace
    *
    * @param m
    * @return
    */
  def ~?(m: Matcher) = concat(Whitespace.?.concat(m))

  def alternate(m: Matcher): Matcher = Alternate(this, m)

  def ?(): Matcher = opt

  def opt: Matcher = Optional(this)

  def |(m: Matcher) = alternate(m)

  def -() = Discard(this)

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

  def remainderOffset = offset + matched.length

  override def startPosition: InputPosition = OffsetInputPosition(offset)

  override def endPosition: InputPosition = OffsetInputPosition(offset + matched.length)

  def remainder: CharSequence = input.subSequence(remainderOffset, input.length())

}

/**
  * Concatenate two patterns
  *
  * @param left  left pattern
  * @param right right pattern
  */
case class Concat(left: Matcher, right: Matcher, name: String = "Concat") extends Matcher {

  override def matchPrefix(offset: Int, input: CharSequence): Option[PatternMatch] = {
    val l = left.matchPrefix(offset, input)
    l match {
      case None =>
        // We're done. It cannot match.
        None
      case Some(leftMatch) =>
        // So far so good
        right.matchPrefix(leftMatch.remainderOffset, input) match {
          case None =>
            // We're done. Right doesn't match.
            None
          case Some(rightMatch) =>
            val mergedTree: Option[MatchedNode] = (leftMatch.node, rightMatch.node) match {
              case (None, None) => None
              case (Some(l), None) => Some(l)
              case (None, Some(r)) => Some(r)
              case (Some(l), Some(r)) =>
                val mergedFields = (l match {
                  case ctn: ContainerTreeNode => ctn.childNodes
                  case n => Seq(n)
                }) ++ (r match {
                  case ctn: ContainerTreeNode => ctn.childNodes
                  case n => Seq(n)
                })
                Some(new SimpleMutableContainerTreeNode("~", mergedFields, l.startPosition, r.endPosition))
            }
            Some(PatternMatch(mergedTree, offset, leftMatch.matched + rightMatch.matched, input, this.toString))
        }
    }
  }
}

/**
  * Try first to match the left pattern, then the right
  *
  * @param left  left pattern
  * @param right right pattern
  */
case class Alternate(left: Matcher, right: Matcher, name: String = "alternate") extends Matcher {

  override def matchPrefix(offset: Int, s: CharSequence): Option[PatternMatch] = {
    val l = left.matchPrefix(offset, s)
    l match {
      case None =>
        right.matchPrefix(offset, s)
      case Some(leftMatch) => Some(leftMatch)
    }
  }
}

/**
  * Match but discard the node output of the matcher
  *
  * @param m
  */
case class Discard(m: Matcher, name: String = "discard") extends Matcher {

  override def matchPrefix(offset: Int, input: CharSequence): Option[PatternMatch] =
    m.matchPrefix(offset, input).map(matched => matched.copy(node = None))

}

case class Optional(m: Matcher, name: String = "optional") extends Matcher {

  override def matchPrefix(offset: Int, s: CharSequence): Option[PatternMatch] = {
    m.matchPrefix(offset, s) match {
      case None =>
        Some(PatternMatch(None, offset, "", s, this.toString))
      case Some(there) => Some(there)
    }
  }
}

case class Rep(m: Matcher, name: String = "rep") extends Matcher {

  override def matchPrefix(offset: Int, s: CharSequence): Option[PatternMatch] = {
    m.matchPrefix(offset, s) match {
      case None =>
        Some(PatternMatch(None, offset, "", s, this.toString))
      case Some(there) => Some(there)
      // TODO KEEP MATCHING
    }
  }
}

object Repsep {

  def apply(m: Matcher, sep: Matcher): Matcher =
    m.? ~? Rep(sep ~? m)

}
