package com.atomist.tree.pathexpression

import com.atomist.rug.spi.TypeRegistry
import com.atomist.tree.TreeNode
import com.atomist.tree.pathexpression.ExecutionResult.ExecutionResult

/**
  * Result type and utility methods of a node navigation.
  */
object ExecutionResult {

  type ExecutionResult = Either[String, List[TreeNode]]

  def apply(nodes: Seq[TreeNode]): ExecutionResult =
    Right(nodes.distinct.toList)

  val empty: ExecutionResult = Right(Nil)

  def show(er: ExecutionResult): String = er match {
    case Right(nodes) => s"\t${nodes.map(show).mkString("\n\t")}"
    case Left(err) => s"[$err]"
  }

  def show(n: TreeNode): String = {
    s"${n.nodeName}:${n.nodeType}"
  }

}

object ExpressionEngine {

  type NodePreparer = TreeNode => TreeNode

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
    * @param typeRegistry type registry, which may contain user-specific types
    * @param nodePreparer called on nodes before any methods (including navigation)
    *                     are called on them. This can be used to set state.
    * @return
    */
  def evaluate(node: TreeNode,
               parsed: PathExpression,
               typeRegistry: TypeRegistry,
               nodePreparer: Option[NodePreparer] = None
              ): ExecutionResult

}
