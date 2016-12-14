package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.content.text.grammar.MatchListener
import com.atomist.tree.content.text.{SimpleMutableContainerTreeNode, MutableContainerTreeNode}

/**
  * A microgrammar instance reflects a grammar that will match part of all of an input.
  * See https://web.stanford.edu/~mlfbrown/paper.pdf
  */
trait Microgrammar {

  /**
    * Match input separated.
    *
    * @param input the string to match
    * @param l listener to be notified on matches. Useful for test infrastructure etc
    * @return a sequence of MutableContainerTreeNode's
    */
  def findMatches(input: CharSequence, l: Option[MatchListener] = None): Seq[MutableContainerTreeNode]

  /**
    * Match all input, which must exactly match input.
    */
  def strictMatch(input: CharSequence, l: Option[MatchListener] = None): MutableContainerTreeNode

  /**
    * Return a single container object holding all matches.
    */
  def matchesInContainer(input: CharSequence, l: Option[MatchListener] = None): MutableContainerTreeNode = {
    val matches = findMatches(input, l)
    val container = SimpleMutableContainerTreeNode.wholeInput("container", matches, input.toString)
    container
  }
}
