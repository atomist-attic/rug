package com.atomist.tree.content.text.microgrammar

import com.atomist.util.lang.JavaHelpers

/**
  * Manages a set of StatePredicates
  */
class StatePredicateManager extends InputConsumer {

  import StatePredicateManager._

  private var predicates: Map[String, StatePredicate[_]] = Map()

  def register(p: StatePredicate[_]): Unit = predicates += (nameFor(p) -> p)

  def registeredPredicates: Set[String] = predicates.keySet

  def registered(predicateName: String): Boolean = predicates.contains(predicateName)

  override def consume(c: Char): Unit = predicates.values.foreach(_.consume(c))

  def valueOf[R](predicateName: String): Option[R] = predicates.get(predicateName).map(p => p.state) match {
    case or: Option[R @unchecked] => or
    case x =>
      println(s"Warning: No predicate value or bad value found for [$predicateName]($x)")
      None
  }
}

object StatePredicateManager {

  def nameFor(p: StatePredicate[_]): String = JavaHelpers.lowerize(p.getClass.getSimpleName)

}