package com.atomist.tree.marshal

import com.atomist.graph.GraphNode

/**
  * Strategy to resolve unresolved nodes
  */
trait NodeResolver {

  /**
    * Resolve this node if possible, ensuring it's
    * ready to run the path expression against
    *
    * @param from         node to navigate from
    * @param unresolvable info about unresolvable node
    * @return node if it can be resolved in this resolver
    */
  def resolve(from: GraphNode,
              unresolvable: Unresolvable,
              relationshipName: String): Option[GraphNode]

}


/**
  * Uses multiple strategies to resolve an unresolvable node
  */
class MultiNodeResolver(resolvers: Seq[NodeResolver]) extends NodeResolver {

  override def resolve(from: GraphNode, unresolvable: Unresolvable, relationshipName: String): Option[GraphNode] = {
    val resolved: Stream[Option[GraphNode]] = resolvers.toStream.map(_.resolve(from, unresolvable, relationshipName))
    resolved.find(_.isDefined).flatten
  }
}
