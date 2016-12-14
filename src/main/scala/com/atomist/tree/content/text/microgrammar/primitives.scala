package com.atomist.tree.content.text.microgrammar

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

case class Renamed(m: Matcher, newName: String) extends Matcher {

  override def name: String = newName

  override def matchPrefix(offset: Int, input: CharSequence): Option[PatternMatch] =
    m.matchPrefix(offset, input).map(matched => matched.copy(node = None))

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
