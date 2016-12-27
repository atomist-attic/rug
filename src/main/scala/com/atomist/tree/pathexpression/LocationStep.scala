package com.atomist.tree.pathexpression

import com.atomist.rug.spi.TypeRegistry
import com.atomist.tree.TreeNode
import com.atomist.tree.pathexpression.ExecutionResult.ExecutionResult

case class LocationStep(axis: AxisSpecifier, test: NodeTest, predicate: Option[Predicate] = None) {

  import ExpressionEngine.NodePreparer

  def follow(tn: TreeNode, typeRegistry: TypeRegistry, nodePreparer: NodePreparer): ExecutionResult =
    test.follow(tn, axis, typeRegistry) match {
      case Right(nodes) => Right(
        nodes
          .map(nodePreparer)
          .filter(tn => predicate.getOrElse(TruePredicate)(tn, nodes))
      )

      case failure => failure
    }
}
