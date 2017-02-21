package com.atomist.tree.pathexpression

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.dynamic.ChildResolver
import com.atomist.rug.spi.TypeRegistry
import com.atomist.tree.AddressableTreeNode
import com.atomist.tree.pathexpression.ExecutionResult.ExecutionResult
import com.atomist.util.misc.SerializationFriendlyLazyLogging

/**
  * Return all nodes of the given type
  *
  * @param tag name of the type we're looking into. An incorrect
  *            type name will result in error evaluations of
  *            path expressions unless the node name starts with lower case,
  *            in which case it may be a local type from a microgrammar etc.
  */
case class NodesWithTag(tag: String)
  extends NodeTest
    with SerializationFriendlyLazyLogging {

  private def childResolver(typeRegistry: TypeRegistry): Option[ChildResolver] =
    typeRegistry.findByName(tag) match {
      case Some(cr: ChildResolver) =>
        Some(cr)
      case _ => None
    }

  private val eligibleNode: GraphNode => Boolean = n => n.nodeTags.contains(tag) || n.nodeName == tag

  // Attempt to find nodes of the require type under the given node
  private def findMeUnder(tn: GraphNode, typeRegistry: TypeRegistry): Option[Seq[GraphNode]] =
    tn.relatedNodes.filter(eligibleNode) match {
      case Nil =>
        childResolver(typeRegistry) match {
          case Some(cr) => Some(cr.findAllIn(tn).getOrElse(Nil))
          case None if tag.charAt(0).isLower =>
            // It starts with lower case. Give it the benefit of the doubt.
            // It might be a microgrammar reference.
            Some(Nil)
          case None =>
            None
        }
      case kids => Some(kids)
    }

  override def follow(tn: GraphNode, axis: AxisSpecifier, ee: ExpressionEngine, typeRegistry: TypeRegistry): ExecutionResult = {
    axis match {
      case NavigationAxis(propertyName) =>
        val nodes = tn.relatedNodesNamed(propertyName).filter(eligibleNode)
        ExecutionResult(nodes)
      case Self =>
        ExecutionResult(List(tn).filter(eligibleNode))
      case Child =>
        findMeUnder(tn, typeRegistry)
          .map(ExecutionResult(_))
          .getOrElse(Left(s"Unknown type [$tag]"))
      case Descendant =>
        val allDescendants = Descendant.selfAndAllDescendants(tn)
        val found = allDescendants.flatMap(d => findMeUnder(d, typeRegistry)).flatten

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
