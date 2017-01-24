package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.content.text.grammar.MatchListener
import com.atomist.tree.content.text.{SimpleMutableContainerTreeNode, MutableContainerTreeNode}

/**
  * A microgrammar instance reflects a grammar that will match part or all of input.
  * Microgrammars are designed to be lossy, pulling out only the parts of the file--and
  * the bind variables--they care about. Unlike in the Stanford paper, our microgrammar
  * results are normally updatable.
  * See https://web.stanford.edu/~mlfbrown/paper.pdf
  */
trait Microgrammar {

  def name: String

  /**
    * Match input separated.
    *
    * @param input the string to match
    * @param l listener to be notified on matches. Useful for test infrastructure etc
    * @return a sequence of MutableContainerTreeNode's
    */
  def findMatches(input: CharSequence, l: Option[MatchListener] = None): Seq[MutableContainerTreeNode]

  /**
    * Return a single container object holding all matches.
    */
  def matchesInContainer(input: CharSequence, l: Option[MatchListener] = None): MutableContainerTreeNode = {
    val matches = findMatches(input, l)
    val container = SimpleMutableContainerTreeNode.wholeInput("container", matches, input.toString)
    container
  }
}
