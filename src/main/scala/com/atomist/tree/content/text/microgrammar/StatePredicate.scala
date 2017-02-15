package com.atomist.tree.content.text.microgrammar

/**
  * Immutable consumer that goes through input one character at a time and
  * the resulting state
  */
trait InputConsumer {

  /**
    * Offset matched
    *
    * @return
    */
  def offset: Int

  def consume(c: Char): InputConsumer

}

/**
  * Report a state that can change as input is consumed
  *
  * @tparam R type of the returned predicate
  */
trait StatePredicate[R <: Any] extends InputConsumer {

  def state: R

  override def consume(c: Char): StatePredicate[R]

}