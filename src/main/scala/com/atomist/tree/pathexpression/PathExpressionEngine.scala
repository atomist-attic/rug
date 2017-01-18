package com.atomist.tree.pathexpression

import com.atomist.rug.spi.TypeRegistry
import com.atomist.tree.TreeNode
import com.atomist.tree.pathexpression.ExecutionResult._

/**
  * Expression engine implementation for our path format
  */
class PathExpressionEngine extends ExpressionEngine {

  import ExpressionEngine.NodePreparer

  override def evaluate(node: TreeNode,
                        parsed: PathExpression,
                        typeRegistry: TypeRegistry,
                        nodePreparer: Option[NodePreparer]): ExecutionResult = {
    var nodesToApplyNextStepTo: ExecutionResult = ExecutionResult(List(node))
    for (locationStep <- parsed.locationSteps) {
      val nextNodes = nodesToApplyNextStepTo match {
        case Right(n :: Nil) =>
          val next: ExecutionResult = locationStep.follow(n, this, typeRegistry, nodePreparer.getOrElse(n => n))
          next
        case Right(Nil) =>
          ExecutionResult(Nil)
        case Right(seq) =>
          val kids: List[TreeNode] = seq
            .flatMap(kid =>
              locationStep.follow(kid, this, typeRegistry, nodePreparer.getOrElse(n => n))
                .right.toOption)
            .flatten
          ExecutionResult(kids)
        case failure@Left(msg) => failure
      }
      //println(s"After evaluating $locationStep on $nodesToApplyNextStepTo we have $nextNodes")
      nodesToApplyNextStepTo = nextNodes
    }
    //println(s"Returning $nodesToApplyNextStepTo when evaluating [$parsed] against $node")
    nodesToApplyNextStepTo
  }
}
