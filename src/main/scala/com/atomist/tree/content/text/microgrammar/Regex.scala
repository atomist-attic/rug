package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.TreeNode
import com.atomist.tree.content.text.MutableTerminalTreeNode
import com.atomist.tree.content.text.microgrammar.Matcher.MatchPrefixResult

/**
  * Matches a regex.
  */
case class Regex(regex: String, givenName: Option[String] = None, config: MatcherConfig = MatcherConfig())
  extends ConfigurableMatcher {

  private val rex = regex.r

  private val treeNodeSignificance = if (givenName.isDefined) TreeNode.Signal else TreeNode.Noise

  val name: String = givenName.getOrElse(".regex")

  override def matchPrefixInternal(inputState: InputState): MatchPrefixResult =
    if (!inputState.exhausted) {
      rex.anchored.findPrefixMatchOf(inputState.remainder) match {
        case Some(m) =>
          Right(PatternMatch(
              new MutableTerminalTreeNode(name, m.matched, inputState.inputPosition, significance = treeNodeSignificance),
            m.matched,
            inputState.take(m.matched.length)._2,
            this.toString))
        case _ => Left(DismatchReport(s"Regex $regex did not match ${inputState.remainder.toString.substring(0, Math.min(40, inputState.remainder.length))}"))
      }
    }
    else
      Left(DismatchReport("we have reached the end"))
}


object Whitespace extends Discard(Regex("""\s+""", None))

object WhitespaceOrNewLine extends Discard(Regex("""[\s\n]+""", Some("whitespace-or-newline")))