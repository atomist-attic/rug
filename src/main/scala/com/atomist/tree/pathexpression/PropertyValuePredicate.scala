package com.atomist.tree.pathexpression

import com.atomist.graph.GraphNode
import com.atomist.rug.spi.TypeRegistry
import com.atomist.tree.TreeNode
import com.atomist.tree.pathexpression.ExpressionEngine.NodePreparer
import com.atomist.tree.utils.NodeUtils

case class PropertyValuePredicate(property: String, expectedValue: String)
  extends Predicate {

  override def toString: String = s"@$property=$expectedValue"

  override def evaluate(n: GraphNode,
                        returnedNodes: Seq[GraphNode],
                        ee: ExpressionEngine,
                        typeRegistry: TypeRegistry,
                        nodePreparer: Option[NodePreparer]): Boolean = {
    if (property == "value") {
      // Treat the value property specially
      n match {
        case tn: TreeNode => tn.value == expectedValue
        case _ => false
      }
    }
    else {
      val extracted = n.relatedNodesNamed(property)
      if (extracted.size == 1) {
        val result = extracted.head match {
          case tn: TreeNode => tn.value.equals(expectedValue)
          case _ => false
        }
        result
      }
      else
        NodeUtils.invokeMethodIfPresent[String](n, property).contains(expectedValue)
    }
  }
}
