package com.atomist.tree.pathexpression

import com.atomist.tree.TreeNode
import com.atomist.tree.pathexpression.ExecutionResult.ExecutionResult

object ExecutionResult {

  type ExecutionResult = Either[String, List[TreeNode]]

  def apply(nodes: Seq[TreeNode]): ExecutionResult =
    Right(nodes.distinct.toList)

  val empty: ExecutionResult = Right(Nil)

  def show(er: ExecutionResult): String = er match {
    case Right(nodes) => s"\t${nodes.map(show).mkString("\n\t")}"
    case Left(err) => s"[$err]"
  }

  def show(n: TreeNode) = {
    s"${n.nodeName}:${n.nodeType}"
  }

}

object ExpressionEngine {

  type NodePreparer = TreeNode => TreeNode

}

trait ExpressionEngine {

  import ExpressionEngine._

  def evaluateParsed(node: TreeNode, parsed: PathExpression, nodePreparer: Option[NodePreparer] = None): ExecutionResult

  /**
    * Return the result of evaluating the expression. If the expression is invalid
    * return a message, otherwise the result of invoking the valid expression.
    *
    * @param node
    * @param expression
    * @param nodePreparer called on nodes before any methods (including navigation)
    *                     are called on them. This can be used to set state.
    * @return
    */
  def evaluate(node: TreeNode, expression: String, nodePreparer: Option[NodePreparer] = None): ExecutionResult

}
