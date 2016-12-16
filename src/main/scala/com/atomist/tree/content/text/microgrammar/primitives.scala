package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.content.text.SimpleMutableContainerTreeNode

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
  * @param m    matcher whose node result we'll discard
  * @param name name of this discarding matcher
  */
case class Discard(m: Matcher, name: String = "discard") extends Matcher {

  override def matchPrefix(offset: Int, input: CharSequence): Option[PatternMatch] =
    m.matchPrefix(offset, input).map(matched => matched.copy(node = None))

}

case class Renamed(m: Matcher, newName: String) extends Matcher {

  override def name: String = newName

  override def matchPrefix(offset: Int, input: CharSequence): Option[PatternMatch] =
    m.matchPrefix(offset, input).map(matched => matched.copy(node = None))

}

/**
  * Wrap the matcher with a new name and in a higher level container node
  *
  * @param m    matcher to wrap
  * @param name new name that will be used for the matcher and the new node level
  */
case class Wrap(m: Matcher, name: String)
  extends Matcher {

  override def matchPrefix(offset: Int, input: CharSequence): Option[PatternMatch] =
    m.matchPrefix(offset, input).map(matched =>
      matched.copy(node = matched.node.map(mn => {
        val n = SimpleMutableContainerTreeNode.wrap(name, mn)
        //println(s"New node is $n")
        n
      })))

}

case class Optional(m: Matcher, name: String = "optional") extends Matcher {

  override def matchPrefix(offset: Int, s: CharSequence): Option[PatternMatch] =
    m.matchPrefix(offset, s) match {
      case None =>
        Some(PatternMatch(None, offset = offset, matched = "", s, this.toString))
      case Some(there) =>
        Some(there)
    }
}
