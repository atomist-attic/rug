package com.atomist.tree.content.text.microgrammar

import com.atomist.tree.content.text.{InputPosition, OffsetInputPosition}
import com.atomist.util.lang.JavaHelpers

import scala.annotation.tailrec

/**
  * Represents the state of the input we're consuming.
  * Keeps track of offset.
  * Manages a set of StatePredicates.
  * Immutable: Consuming input returns a new instance
  */
case class InputState(
                       input: CharSequence,
                       predicates: Map[String, StatePredicate[_]] = Map(),
                       offset: Int = 0,
                       knownMatchers: Map[String, Matcher] = Map())
  extends InputConsumer {

  import InputState._

  def inputPosition: InputPosition = OffsetInputPosition(offset)

  def register(p: StatePredicate[_]): InputState =
    InputState(input, predicates ++ Map(nameFor(p) -> p))

  def exhausted: Boolean = offset > input.length() - 1

  def advance: InputState = takeOne._2

  private def takeOne: (Char, InputState) = {
    val frontChar = input.charAt(offset)
    (frontChar, consume(frontChar))
  }

  /**
    * Consume the given number of characters, if possible.
    * @param n
    * @return
    */
  def take(n: Int): (String, InputState) = InputState.take(this, n)

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

  /*
   * Notice all the state changes that this character causes.
   */
  override def consume(c: Char): InputState = {
    val updatedPredicates: Map[String, StatePredicate[_]] = predicates.map {
      case (k, v) => (k, v.consume(c))
    }.toMap
    InputState(input, updatedPredicates, offset + 1, knownMatchers)
  }

  def valueOf[R](predicateName: String): Option[R] = predicates.get(predicateName).map(p => p.state) match {
    case or: Option[R@unchecked] => or
    case x =>
      println(s"Warning: No predicate value or bad value found for [$predicateName]($x)")
      None
  }

  override def toString: String = s"InputState(Offset ${offset} in input of length ${input.length} with predicates ${predicates})"
}

object InputState2 {

  def unapply(inputState: InputState): Option[(CharSequence, Map[String, StatePredicate[_]], Int)] =
    Some((inputState.input, inputState.predicates, inputState.offset))
}

object InputState {

  def nameFor(p: StatePredicate[_]): String = JavaHelpers.lowerize(p.getClass.getSimpleName)

  def take(inputState: InputState, n: Int): (String, InputState) = {
    @tailrec
    def takeEach(n: Int, characters: Seq[Char], state: InputState): (Seq[Char], InputState) =
      if (state.exhausted || n <= 0)
        (characters, state)
      else {
        val (consumedChar, newState) = state.takeOne
        takeEach(n - 1, characters :+ consumedChar, newState)
      }

    val (characters, state) = takeEach(n, Seq(), inputState)
    (characters.mkString, state)
  }

}