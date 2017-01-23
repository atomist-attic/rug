package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.content.text.{InputPosition, OffsetInputPosition}
import com.atomist.util.lang.JavaHelpers

/**
  * Represents the state of the input we're consuming.
  * Keeps track of offset.
  * Manages a set of StatePredicates.
  * Immutable: Consuming input returns a new instance
  */
case class InputState(
                       input: CharSequence,
                       predicates: Map[String, StatePredicate[_]] = Map(),
                       offset: Int = 0)
  extends InputConsumer {

  import InputState._

  def inputPosition: InputPosition = OffsetInputPosition(offset)

  def register(p: StatePredicate[_]): InputState =
    InputState(input, predicates ++ Map(nameFor(p) -> p))

  def exhausted: Boolean = offset > input.length() - 1

  def advance: InputState = take(1)._2

  /**
    * Consume the given number of characters, if possible.
    * @param n
    * @return
    */
  def take(n: Int): (String, InputState) = {
    var newme = this
    var s = ""
    for {
      i <- 0 until n
      if !newme.exhausted
    } {
      val c = newme.input.charAt(newme.offset)
      s += c.toString
      //println(s"Consumed $c from ${newme.input} at ${newme.offset} with s =[$s]")
      val updated = newme.consume(c)
      //println(s"Newme now=$newme")
      require(updated.offset == newme.offset + 1)
      newme = updated
    }
    //println(s"Take $n on [$input] returned ([$s, $newme)")
    (s, newme)
  }

  /**
    * Consume all input
    * @return
    */
  def takeAll: (String,InputState) = take(Int.MaxValue)

  /**
    * Remainder of the input
    * @return
    */
  def remainder: CharSequence = input.subSequence(offset, input.length())

  def registeredPredicates: Set[String] = predicates.keySet

  def registered(predicateName: String): Boolean = predicates.contains(predicateName)

  override def consume(c: Char): InputState = {
    val updatedPredicates: Map[String, StatePredicate[_]] = predicates.map {
      case (k, v) => (k, v.consume(c))
    }.toMap
    InputState(input, updatedPredicates, offset + 1)
  }

  def valueOf[R](predicateName: String): Option[R] = predicates.get(predicateName).map(p => p.state) match {
    case or: Option[R@unchecked] => or
    case x =>
      println(s"Warning: No predicate value or bad value found for [$predicateName]($x)")
      None
  }
}

object InputState {

  def nameFor(p: StatePredicate[_]): String = JavaHelpers.lowerize(p.getClass.getSimpleName)

}