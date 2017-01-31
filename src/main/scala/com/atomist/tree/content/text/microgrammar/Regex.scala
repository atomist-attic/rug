package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.TreeNode
import com.atomist.tree.content.text.MutableTerminalTreeNode
import com.atomist.tree.content.text.microgrammar.Matcher.MatchPrefixResult

/**
  * Matches a regex.
  */
case class Regex(regex: String, givenName: Option[String], config: MatcherConfig = MatcherConfig())
  extends ConfigurableMatcher {
  import Regex._

  // TODO look at greedy

  private val rex = regex.r

  private val treeNodeSignificance = if (givenName.isDefined) TreeNode.Signal else TreeNode.Noise
  val name = givenName.getOrElse(DefaultRegexName)

  override def matchPrefixInternal(inputState: InputState): MatchPrefixResult =
    if (!inputState.exhausted) {
      rex.anchored.findPrefixMatchOf(inputState.remainder) match {
        case Some(m) =>
          Right(PatternMatch(
            Some(
              new MutableTerminalTreeNode(name, m.matched, inputState.inputPosition, significance = treeNodeSignificance)),
            m.matched,
            inputState.take(m.matched.length)._2,
            this.toString))
        case _ => Left(DismatchReport(s"Regex $regex did not match ${inputState.remainder.toString.substring(0, Math.min(40, inputState.remainder.length))}"))
      }
    }
    else
      Left(DismatchReport("we have reached the end"))
}

object Regex {
  @deprecated
  def apply(name: String, regex: String): Regex = Regex(regex, Some(name))

  val DefaultRegexName = ".regex"
}

/**
  * Matcher that returns an empty placeholder.
  */
class Placeholder extends Matcher {

  override def name: String = "placeholder"

  // what does this accomplish if we can't name it?

  override def matchPrefixInternal(inputState: InputState): MatchPrefixResult =
    if (!inputState.exhausted) {
      Right(PatternMatch(None, "", inputState, this.toString))
    }
    else
      Left(DismatchReport("no input remains")) // why can't we put a placeholder at the very end?
}

object Whitespace extends Discard(Regex("""\s+""", None))

object WhitespaceOrNewLine extends Discard(Regex("whitespace-or-newline", """[\s\n]+"""))