package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.MutableTreeNode
import com.atomist.tree.content.text._
import com.atomist.tree.content.text.microgrammar.Matcher.MatchPrefixResult
import com.atomist.tree.content.text.microgrammar.matchers.Break

case class MatcherConfig(greedy: Boolean = true)

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
  protected def matchPrefixInternal(inputState: InputState): MatchPrefixResult

  final def matchPrefix(is: InputState): MatchPrefixResult = {
    val matchedOption = matchPrefixInternal(is)

    matchedOption.right.foreach { matchFound =>
      matchFound.node.asInstanceOf[MutableTreeNode].addType(name)
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

object Matcher {

  type MatchPrefixResult = Either[DismatchReport, PatternMatch]

  def prettyPrint(m: Matcher): String = prettyPrintLines(m).mkString("\n")

  private def prettyPrintLines(m: Matcher): Seq[String] = {

    def nameOrNot(named: Option[String]): String = named.map(_ + " = ").getOrElse("")

    def mkSeq(pre: String, in: Seq[String], post: String = ")") = Seq(pre) ++ in ++ Seq(post)

    val lines: Seq[String] = m match {
      case Alternate(left, right, name) =>
        mkSeq(s"Alternate($name",
          prettyPrintLines(left) ++
            prettyPrintLines(right))
      case Literal(literal, named) => Seq(s"Literal(${nameOrNot(named)}$literal)")
      case Rep(m, name, separator) =>
        mkSeq(s"Rep($name",
          prettyPrintLines(m) ++
            separator.map(sm => s" separated by ${prettyPrintLines(sm)}"))
      case Break(breakToMatcher, named) =>
        mkSeq(s"Break(${nameOrNot(named)}", prettyPrintLines(breakToMatcher))
      case Concat(left, right, name) =>
        mkSeq(s"Concat($name",
          prettyPrintLines(left) ++
            prettyPrintLines(right))
      case Discard(m, name) =>
        mkSeq(s"Discard($name", prettyPrintLines(m))
      case Wrap(m, name) =>
        mkSeq(s"Wrap($name", prettyPrintLines(m))
      case RestOfLine(name) =>
        Seq(s"RestOfLine($name)")
      case Optional(m, name) =>
        mkSeq(s"Optional($name", prettyPrintLines(m))
      case Reference(delegate, name) =>
        mkSeq(s"Reference($name", prettyPrintLines(delegate))
      case other =>
        Seq(other.toString)
    }

    lines.map(l => " " + l)
  }
}


/**
  * Trait extended by Matchers that are configurable.
  */
trait ConfigurableMatcher extends Matcher {

  def config: MatcherConfig
}

/**
  * Returned for a pattern match.
  *
  * @param node                matched node. If None, the match is discarded.
  * @param matched             the string that was matched
  * @param resultingInputState resulting InputState
  */
case class PatternMatch(
                         node: PositionedTreeNode,
                         matched: String,
                         resultingInputState: InputState,
                         matcherId: String)
  extends Positioned {

  def remainderOffset: Int = resultingInputState.offset + matched.length

  override def startPosition: InputPosition = endPosition - matched.length

  override def endPosition: InputPosition = resultingInputState.inputPosition

  def remainder: CharSequence = resultingInputState.input.subSequence(remainderOffset, resultingInputState.input.length())

}
