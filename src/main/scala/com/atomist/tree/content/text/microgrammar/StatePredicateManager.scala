package com.atomist.tree.content.text.microgrammar

import com.atomist.util.lang.JavaHelpers

/**
  * Manages a set of StatePredicates
  */
case class StatePredicateManager(
                                  predicates: Map[String, StatePredicate[_]] = Map(),
                                  offset: Int = 0)
  extends InputConsumer {

  import StatePredicateManager._

  def register(p: StatePredicate[_]): StatePredicateManager =
    StatePredicateManager(predicates ++ Map(nameFor(p) -> p))

  def registeredPredicates: Set[String] = predicates.keySet

  def registered(predicateName: String): Boolean = predicates.contains(predicateName)

  override def consume(c: Char): StatePredicateManager = {
    val updatedPredicates: Map[String, StatePredicate[_]] = predicates.map {
      case (k, v) => (k, v.consume(c))
    }.toMap
    StatePredicateManager(updatedPredicates, offset + 1)
  }

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