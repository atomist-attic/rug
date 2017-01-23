package com.atomist.tree.content.text.microgrammar.predicates

import com.atomist.tree.content.text.microgrammar.StatePredicate

/**
  * Predicate that keeps track of curlyDepth
  */
case class CurlyDepth(offset: Int, depth: Int,
                      inString: InString)
  extends StatePredicate[Int] {

  def this() =
    this(0, 0, InString())

  override def state: Int = depth

  // TODO escaping

  override def consume(c: Char): CurlyDepth = {
    val newInstring = inString.consume(c)
    val newDepth = c match {
      case '{' if !inString.state =>
        depth + 1
      case '}' if !inString.state =>
        depth - 1
      case _ =>
        depth
    }
    CurlyDepth(offset + 1, newDepth, newInstring)
  }
}
