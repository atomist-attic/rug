package com.atomist.tree.pathexpression

import com.atomist.rug.spi.TypeRegistry
import com.atomist.tree.TreeNode
import com.atomist.tree.pathexpression.ExecutionResult._

object PathExpressionEngine {

  val DotSeparator = "."

  val SlashSeparator = "/"

  val AmongSeparator = "$"

  val SlashSlash = "//"

  val PredicateOpen = "["

  val PredicateClose = "]"

}

/**
  * Expression engine implementation for our path format
  */
class PathExpressionEngine extends ExpressionEngine {

  import ExpressionEngine.NodePreparer

  override def evaluate(node: TreeNode, parsed: PathExpression,
                        typeRegistry: TypeRegistry,
                        nodePreparer: Option[NodePreparer]): ExecutionResult = {
    var nodesToApplyNextStepTo: ExecutionResult = ExecutionResult(List(node))
    for (locationStep <- parsed.locationSteps) {
      val nextNodes = nodesToApplyNextStepTo match {
        case Right(n :: Nil) =>
          val next: ExecutionResult = locationStep.follow(n, typeRegistry, nodePreparer.getOrElse(n => n))
          next
        case Right(Nil) =>
          ExecutionResult(Nil)
        case Right(seq) =>
          val kids: List[TreeNode] = seq
            .flatMap(kid =>
              locationStep.follow(kid, typeRegistry, nodePreparer.getOrElse(n => n))
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

case class PathExpression(
                           locationSteps: Seq[LocationStep]
                         )