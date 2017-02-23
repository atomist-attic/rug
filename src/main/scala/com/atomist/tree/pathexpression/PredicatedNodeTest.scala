package com.atomist.tree.pathexpression

import com.atomist.graph.GraphNode
import com.atomist.rug.spi.TypeRegistry
import com.atomist.tree.TreeNode
import com.atomist.tree.pathexpression.ExecutionResult.ExecutionResult

/**
  * Convenience superclass that uses a predicate to handle sourced nodes
  *
  * @param predicate predicate used to filter nodes
  */
abstract class PredicatedNodeTest(name: String, predicate: Predicate) extends NodeTest {

  final override def follow(tn: GraphNode, axis: AxisSpecifier, ee: ExpressionEngine, typeRegistry: TypeRegistry): ExecutionResult =
    sourceNodes(tn, axis, typeRegistry) match {
      case Right(nodes) =>
        ExecutionResult(nodes.filter(tn => predicate.evaluate(tn, nodes, ee, typeRegistry, None)))
      case failure => failure
    }

  /**
    * Subclasses can override this to provide a more efficient implementation.
    * This one works but can be expensive.
    */
  protected def sourceNodes(tn: GraphNode, axis: AxisSpecifier, typeRegistry: TypeRegistry): ExecutionResult = axis match {
    case Self => ExecutionResult(List(tn))
    case Child =>
      val kids = tn.relatedNodes.filter {
        case tn: TreeNode => tn.significance != TreeNode.Noise
        case _ => true
      }.toList
      ExecutionResult(kids)
    case Descendant =>
      val kids = (Descendant.allDescendants(tn) filter {
        case tn: TreeNode => tn.significance != TreeNode.Noise
        case _ => true
      }).toList
      ExecutionResult(kids)
    case NavigationAxis(propertyName) =>
      val nodes = tn.relatedNodesNamed(propertyName)
      ExecutionResult(nodes)
    case x => throw new UnsupportedOperationException(s"Unsupported axis $x in ${getClass.getSimpleName}")
  }
}


/**
  * Return all nodes on the given axis
  */
object All extends PredicatedNodeTest("All", TruePredicate) {
  override def toString = "*"
}