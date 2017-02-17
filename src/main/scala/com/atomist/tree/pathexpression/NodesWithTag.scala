package com.atomist.tree.pathexpression

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.dynamic.ChildResolver
import com.atomist.rug.spi.{MutableView, TypeRegistry}
import com.atomist.tree.{AddressableTreeNode, TreeNode}
import com.atomist.tree.pathexpression.ExecutionResult.ExecutionResult
import com.atomist.util.misc.SerializationFriendlyLazyLogging

/**
  * Return all nodes of the given type
  *
  * @param tag name of the type we're looking into
  */
case class NodesWithTag(tag: String)
  extends NodeTest
    with SerializationFriendlyLazyLogging {

  private def childResolver(typeRegistry: TypeRegistry): Option[ChildResolver] =
    typeRegistry.findByName(tag) match {
      case Some(cr: ChildResolver) =>
        Some(cr)
      case Some(_) => throw new IllegalStateException(s"type $tag is in the type registry ($typeRegistry) but does not implement ChildResolver")
      case _ => None
    }

  private val eligibleNode: GraphNode => Boolean = n => n.nodeTags.contains(tag) || n.nodeName == tag

  // Attempt to find nodes of the require type under the given node
  private def findMeUnder(tn: GraphNode, typeRegistry: TypeRegistry): Seq[GraphNode] =
    tn.relatedNodes.filter(eligibleNode) match {
      case Nil =>
        childResolver(typeRegistry).flatMap(cr => cr.findAllIn(tn)).getOrElse(Nil)
      case kids => kids
    }

  override def follow(tn: GraphNode, axis: AxisSpecifier, ee: ExpressionEngine, typeRegistry: TypeRegistry): ExecutionResult = {
    axis match {
      case NavigationAxis(propertyName) =>
        val nodes = tn.relatedNodesNamed(propertyName).filter(eligibleNode)
        ExecutionResult(nodes)
      case Self => ExecutionResult(List(tn).filter(eligibleNode))
      case Child =>
        ExecutionResult(findMeUnder(tn, typeRegistry))
      case Descendant =>
        val allDescendants = Descendant.selfAndAllDescendants(tn)
        val found = allDescendants.flatMap(d => findMeUnder(d, typeRegistry))

        // We may have duplicates in the found collection because, for example,
        // we might find the Java() node SomeClass.java under the directory "/src"
        // and under "/src/main" and under the actual file.
        // So check that our returned types have distinct backing objects
        var nodeAddressesSeen: Set[String] = Set()
        var toReturn = List.empty[GraphNode]
        found.foreach {
          case mv: AddressableTreeNode =>
            if (!nodeAddressesSeen.contains(mv.address)) {
              nodeAddressesSeen = nodeAddressesSeen + mv.address
              toReturn = toReturn :+ mv
            }
          case n =>
            // There's no concept of an address here, so we have to take on trust that
            // we didn't find this thing > once
            toReturn = toReturn :+ n
        }
        ExecutionResult(toReturn)
    }
  }
}
