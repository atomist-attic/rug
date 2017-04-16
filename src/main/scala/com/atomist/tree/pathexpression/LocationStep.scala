package com.atomist.tree.pathexpression

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.core.RepoResolver
import com.atomist.rug.runtime.js.ExecutionContext
import com.atomist.rug.spi.TypeRegistry
import com.atomist.tree.pathexpression.ExecutionResult.ExecutionResult

/**
  * A step within a path expression
  *
  * @param axis       AxisSpecificer: direction of navigation
  * @param test       node test: Narrows nodes
  * @param predicates predicates that will be combined to filter nodes.
  *                   Use the predicateToEvaluate field if you wish to evaluate
  */
case class LocationStep(axis: AxisSpecifier,
                        test: NodeTest,
                        predicates: Seq[Predicate]) {

  import ExpressionEngine.NodePreparer

  def follow(tn: GraphNode, ee: ExpressionEngine, executionContext: ExecutionContext, nodePreparer: NodePreparer): ExecutionResult =
    test.follow(tn, axis, ee, executionContext) match {
      case Right(nodes) => ExecutionResult(
        nodes
          .map(nodePreparer)
          .filter(tn => predicateToEvaluate.evaluate(tn, nodes, ee, executionContext, Some(nodePreparer)))
      )
      case failure => failure
    }

  /**
    * Return a single predicate we can use to evaluate this expression.
    * Combines multiple predicates if necessary.
    * Will return TruePredicate if there is essentially no predicate,
    * so this predicate can always be used in evaluation.
    */
  val predicateToEvaluate: Predicate = combine(predicates)

  private def combine(preds: Seq[Predicate]): Predicate = preds match {
    case Nil => TruePredicate
    case pred :: Nil => pred
    case preds => preds.head and combine(preds.tail)
  }

  override def toString = s"${axis}::$test${predicates.mkString("[","][","]")}"
}

/**
  * Result of parsing a path expression.
  */
case class PathExpression(locationSteps: Seq[LocationStep]) {

  override def toString: String = locationSteps.mkString("/")

  def dropLastStep: PathExpression = copy(locationSteps = locationSteps.dropRight(1))
}