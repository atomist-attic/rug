package com.atomist.tree.pathexpression

import com.atomist.graph.GraphNode
import com.atomist.rug.runtime.js.{DefaultExecutionContext, ExecutionContext}
import com.atomist.tree.pathexpression.ExecutionResult.ExecutionResult

object ExpressionEngine {

  type NodePreparer = GraphNode => GraphNode

}

/**
  * Evaluates path expressions, whether as raw strings or parsed.
  */
trait ExpressionEngine {

  import ExpressionEngine._

  /**
    * Return the result of evaluating the expression. If the expression is invalid
    * return a message, otherwise the result of invoking the valid expression.
    *
    * @param node         root node to evaluate the path against
    * @param parsed       Parsed path expression. It's already been validated
    * @param executionContext context used to resolve repos or anything else
    *                         that needs user context
    * @param nodePreparer called on nodes before any methods (including navigation)
    *                     are called on them. This can be used to set state.
    * @return
    */
  def evaluate(node: GraphNode,
               parsed: PathExpression,
               executionContext: ExecutionContext = DefaultExecutionContext,
               nodePreparer: Option[NodePreparer] = None
              ): ExecutionResult

}
