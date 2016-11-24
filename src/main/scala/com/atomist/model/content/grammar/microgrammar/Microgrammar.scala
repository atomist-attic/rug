package com.atomist.model.content.grammar.microgrammar

import com.atomist.model.content.grammar.MatchListener
import com.atomist.model.content.text.{SimpleMutableContainerTreeNode, MutableContainerTreeNode, SimpleMutableContainerTreeNode$}

/**
  * A microgrammar instance reflects a grammar that will match part of all of an input.
  * See https://web.stanford.edu/~mlfbrown/paper.pdf
  */
trait Microgrammar {

  /**
    * Match input separated.
    *
    * @param input
    * @param l listener to be notified on matches. Useful for test infrastructure etc.
    * @return
    */
  def findMatches(input: String, l: Option[MatchListener] = None): Seq[MutableContainerTreeNode]

  /**
    * Match all input, which must exactly match input.
    */
  def strictMatch(input: String, l: Option[MatchListener] = None): MutableContainerTreeNode

  /**
    * Return a single container object holding all matches.
    */
  def matchesInContainer(input: String, l: Option[MatchListener] = None): MutableContainerTreeNode = {
    val matches = findMatches(input, l)
    val container = SimpleMutableContainerTreeNode.wholeInput("container", matches, input)
    container
  }
}
