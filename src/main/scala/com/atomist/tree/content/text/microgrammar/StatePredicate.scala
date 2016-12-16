package com.atomist.tree.content.text.microgrammar

import com.atomist.util.lang.JavaHelpers

/**
  * Consumer that goes through input one character at a time and updates state
  */
trait InputConsumer {

  def consume(c: Char): Unit

}

/**
  * Report a state that can change as input is consumed
  *
  * @tparam R type of the returned predicate
  */
trait StatePredicate[R <: Any] extends InputConsumer {

  def state: R

}

/**
  * Predicate that keeps track of whether we're in a string
  */
class InString extends StatePredicate[Boolean] {

  private var inString = false

  override def state: Boolean = inString

  override def consume(c: Char): Unit = c match {
    case '"' =>
      inString = !inString
    case _ =>
  }
}

/**
  * Predicate that keeps track of curlyDepth
  */
class CurlyDepth extends StatePredicate[Int] {

  private val inString = new InString

  private var depth = 0

  override def state: Int = depth

  // TODO escaping

  override def consume(c: Char): Unit = {
    inString.consume(c)
    c match {
      case '{' if !inString.state =>
        depth += 1
      case '}' if !inString.state =>
        depth -= 1
      case _ =>
    }
  }
}