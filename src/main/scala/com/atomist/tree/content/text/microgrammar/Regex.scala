package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.content.text.{MutableTerminalTreeNode, OffsetInputPosition}

// TODO Allow capture groups or is this degenerate?

/**
  * Matches a regex.
  */
case class Regex(name: String, regex: String, config: MatcherConfig = MatcherConfig())
  extends TerminalMatcher with ConfigurableMatcher {

  // TODO look at greedy

  private val rex = regex.r

  override def matchPrefix(offset: Int, input: CharSequence): Option[PatternMatch] =
    if (input != null && input.length() > 0) {
      rex.anchored.findPrefixMatchOf(input.subSequence(offset, input.length())) match {
        case Some(m) =>
          Some(PatternMatch(Some(new MutableTerminalTreeNode(name, m.matched, OffsetInputPosition(offset))), offset, m.matched, input, this.toString))
        case _ => None
      }
    }
    else
      None
}

/**
  * Matcher that returns an empty placeholder.
  */
class Placeholder extends Matcher {

  override def name: String = "placeholder"

  override def matchPrefix(offset: Int, s: CharSequence): Option[PatternMatch] =
    if (s != null && s.length() > 0) {
      Some(PatternMatch(null, offset, "", s, this.toString))
    }
    else
      None
}

object Whitespace extends Regex("whitespace", """\s+""")

object WhitespaceOrNewLine extends Regex("whitespace-or-newline", """[\s\n]+""")