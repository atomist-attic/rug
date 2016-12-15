package com.atomist.plan

import com.atomist.event.SystemEvent
import com.atomist.tree.TreeNode
import com.atomist.tree.pathexpression.PathExpression

/**
  * Find the root for an expression. First pass before executing it.
  */
trait TreeMaterializer {

  /**
    * Find the root node for evaluating the given path expression.
    * For example, an implementation may make any necessary queries to
    * materialize the relevant object graph.
    * Does not evaluate the path expression but may use hints to
    * evaluate it.
    * @param e system event
    * @param pe Path expression
    * @return the root node against which the expression should be evaluated.
    */
  def rootNodeFor(e: SystemEvent, pe: PathExpression): TreeNode

}
