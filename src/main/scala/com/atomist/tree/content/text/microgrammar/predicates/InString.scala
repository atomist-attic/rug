package com.atomist.tree.content.text.microgrammar.predicates

import com.atomist.tree.content.text.microgrammar.StatePredicate

/**
  * Predicate that keeps track of whether we're in a string
  */
case class InString(offset: Int = 0, inString: Boolean = false)
  extends StatePredicate[Boolean] {

  override def state: Boolean = inString

  override def consume(c: Char): InString = {
    val resultingState = c match {
      case '"' =>
        !inString
      case _ =>
        inString
    }
    InString(offset + 1, resultingState)
  }
}
