package com.atomist.tree.marshal

import com.atomist.graph.GraphNode
import com.atomist.tree.utils.NodeUtils

/**
  * Supporting unresolvable nodes.
  * We don't use a special node type, but
  * a convention for marking nodes as unresolvable.
  */
object Unresolvable {

  val RemainingPathExpressionKey = "remainingPathExpression"

  val UnresolvableTag = "Unresolvable"

  def apply(n: GraphNode): Option[Unresolvable] =
    n.nodeTags.find(_ == UnresolvableTag).map(tag => {
      Unresolvable(
        NodeUtils.keyValue(n, RemainingPathExpressionKey).getOrElse(
          throw new IllegalArgumentException(
            s"Node $n must have key [$RemainingPathExpressionKey]: Found keys [${n.relatedNodeNames.mkString(",")}]")
        )
      )
    })

}

case class Unresolvable(remainingPathExpression: String)
