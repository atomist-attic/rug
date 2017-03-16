package com.atomist.tree.pathexpression

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.dynamic.ChildResolver
import com.atomist.rug.spi.TypeRegistry
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

  private object NoSuchTypeException extends Exception

  private def childResolver(typeRegistry: TypeRegistry): Option[ChildResolver] =
    typeRegistry.findByName(tag) match {
      case Some(cr: ChildResolver) =>
        Some(cr)
      case Some(_) =>
        // Type is found, but not a ChildResolver. Not an error
        None
      case None if tag.charAt(0).isLower =>
        // It starts with lower case. Give it the benefit of the doubt.
        // It might be a microgrammar reference.
        None
      case _ =>
        // Type is unknown. This is an error
        throw NoSuchTypeException
    }

  private val eligibleNode: GraphNode => Boolean = n => n.nodeTags.contains(tag) || n.nodeName == tag

  // Attempt to find nodes of the require type under the given node
  private def findMeUnder(tn: GraphNode, typeRegistry: TypeRegistry): Seq[GraphNode] =
    tn.relatedNodes.filter(eligibleNode) match {
      case Nil =>
        childResolver(typeRegistry) match {
          case Some(cr) => cr.findAllIn(tn).getOrElse(Nil)
          case None =>
            Nil
        }
      case kids => kids
    }

  override def follow(tn: GraphNode, axis: AxisSpecifier, ee: ExpressionEngine, typeRegistry: TypeRegistry): ExecutionResult = {
    try {
      axis match {
        case NavigationAxis(propertyName) =>
          val nodes = tn.relatedNodesNamed(propertyName).filter(eligibleNode)
          ExecutionResult(nodes)
        case Self =>
          ExecutionResult(List(tn).filter(eligibleNode))
        case Child =>
          ExecutionResult(findMeUnder(tn, typeRegistry))
        case Descendant =>
          val allDescendants = Descendant.selfAndAllDescendants(tn)
          val found: Seq[GraphNode] = allDescendants.flatMap(d => findMeUnder(d, typeRegistry))
          ExecutionResult(found)
        case x => throw new UnsupportedOperationException(s"Unsupported axis $x in ${getClass.getSimpleName}")
      }
    }
    catch {
      case NoSuchTypeException =>
        Left(s"No such type: [$tag]")
    }
  }

}

