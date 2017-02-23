package com.atomist.tree.pathexpression

import com.atomist.graph.GraphNode
import com.atomist.rug.spi.TypeRegistry
import com.atomist.graph.GraphNode
import com.atomist.tree.pathexpression.ExecutionResult._

/**
  * Expression engine implementation for our path format
  */
class PathExpressionEngine extends ExpressionEngine {

  import ExpressionEngine.NodePreparer

  override def evaluate(node: GraphNode,
                        parsed: PathExpression,
                        typeRegistry: TypeRegistry,
                        nodePreparer: Option[NodePreparer]): ExecutionResult = {
    val (result, report) = evaluateAndReport(node, parsed, typeRegistry, nodePreparer)
    // println("Evaluate Report:\n" + report.mkString("\n"))
    result
  }

  private def evaluateAndReport(node: GraphNode,
                                parsed: PathExpression,
                                typeRegistry: TypeRegistry,
                                nodePreparer: Option[NodePreparer]): (ExecutionResult, Seq[String]) = {
    val report = Seq[String]()

    def say(something: => String) = {} //report = report :+ something

    var nodesToApplyNextStepTo: ExecutionResult = ExecutionResult(List(node))
    for (locationStep <- parsed.locationSteps) {
      say(s"Checking location step $locationStep")
      val nextNodes: ExecutionResult = nodesToApplyNextStepTo match {
        case Right(n :: Nil) =>
          // say("why bother with a special case")
          val next: ExecutionResult = locationStep.follow(n, this, typeRegistry, nodePreparer.getOrElse(n => n))
          next
        case Right(s) if s.isEmpty =>
          ExecutionResult(Nil)
        case Right(seq) =>
          say(s"checking ${seq.size} nodes for matches")
          val results: Seq[ExecutionResult] = seq
            .map(kid =>
              locationStep.follow(kid, this, typeRegistry, nodePreparer.getOrElse(n => n)))
          if (results.exists(_.isLeft)) {
            // If there are failures, use the first
            results.find(_.isLeft).head
          }
          else {
            val kids: Seq[GraphNode] = results.flatMap(r => r.right.toOption).flatten
            ExecutionResult(kids)
          }
        case failure@Left(msg) =>
          say(s"Failure: $msg")
          failure
      }
      //  say(s"After evaluating $locationStep on ${nodesToApplyNextStepTo.right.get.map(TreeNodeUtils.toShortString).mkString("\n ... and ... \n")} we have $nextNodes")
      nodesToApplyNextStepTo = nextNodes
    }
    say(s"Returning $nodesToApplyNextStepTo when evaluating [$parsed] against $node")

    (nodesToApplyNextStepTo, report.map(" " + _))
  }

}
