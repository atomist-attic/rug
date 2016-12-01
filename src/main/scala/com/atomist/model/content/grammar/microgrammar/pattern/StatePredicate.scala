package com.atomist.model.content.grammar.microgrammar.pattern

import com.atomist.rug.kind.java.support.JavaHelpers

/**
  * Consumer that goes through input one character at a time and updates state
  */
trait InputConsumer {

  def consume(c: Char): Unit

}

/**
  * Report a state that can change as input is consumed
  *
  * @tparam R
  */
trait StatePredicate[R <: Any] extends InputConsumer {

  def state: R

}


/**
  * Manages a set of StatePredicates
  */
class StatePredicateManager extends InputConsumer {

  import StatePredicateManager._

  private var predicates: Map[String, StatePredicate[_]] = Map()

  def register(p: StatePredicate[_]): Unit = predicates += (nameFor(p) -> p)

  def registeredPredicates: Set[String] = predicates.keySet

  def registered(predicateName: String) = predicates.contains(predicateName)

  override def consume(c: Char): Unit = predicates.values.foreach(_.consume(c))

  def valueOf[R](predicateName: String): Option[R] = predicates.get(predicateName).map(p => p.state) match {
    case or: Option[R] => or
    case x =>
      println(s"Warning: No predicate value or bad vaue found for [$predicateName]($x)")
      None
  }
}

object StatePredicateManager {

  def nameFor(p: StatePredicate[_]) = JavaHelpers.lowerize(p.getClass.getSimpleName)

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