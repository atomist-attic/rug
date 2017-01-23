package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.content.text.MutableTerminalTreeNode

/**
  * Matches a regex.
  */
case class Regex(name: String, regex: String, config: MatcherConfig = MatcherConfig())
  extends TerminalMatcher with ConfigurableMatcher {

  // TODO look at greedy

  private val rex = regex.r

  override def matchPrefix(inputState: InputState): Option[PatternMatch] =
    if (!inputState.exhausted) {
      rex.anchored.findPrefixMatchOf(inputState.remainder) match {
        case Some(m) =>
          Some(PatternMatch(
            Some(
              new MutableTerminalTreeNode(name, m.matched, inputState.inputPosition)),
            m.matched,
            inputState.take(m.matched.length)._2,
            this.toString))
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

  override def matchPrefix(inputState: InputState): Option[PatternMatch] =
    if (!inputState.exhausted) {
      Some(PatternMatch(None, "", inputState, this.toString))
    }
    else
      None
}

object Whitespace extends Discard(Regex("whitespace", """\s+"""))

object WhitespaceOrNewLine extends Discard(Regex("whitespace-or-newline", """[\s\n]+"""))