package com.atomist.tree

import com.atomist.graph.GraphNode
import com.atomist.rug.runtime.SystemEvent
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
  def rootNodeFor(e: SystemEvent, pe: PathExpression): GraphNode

  /**
    * Given a root node, hydrate it in order to be able to evaluate the given path expression.
    *
    * @return the hydrated root node against which the expression should be evaluated.
    */
  def hydrate(teamId: String, rawRootNode: GraphNode, pe: PathExpression): GraphNode
}

/**
  * TreeMaterializer that never retrieves extra data.
  */
object IdentityTreeMaterializer extends TreeMaterializer {

  override def rootNodeFor(e: SystemEvent, pe: PathExpression): GraphNode = ???

  override def hydrate(teamId: String, rawRootNode: GraphNode, pe: PathExpression): GraphNode = {
    // println(s"Hydrating $rawRootNode")
    rawRootNode
  }
}