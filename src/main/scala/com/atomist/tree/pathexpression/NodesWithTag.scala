package com.atomist.tree.pathexpression

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.dynamic.ChildResolver
import com.atomist.rug.runtime.js.ExecutionContext
import com.atomist.rug.spi.TypeRegistry
import com.atomist.tree.TreeNode
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

  private def childResolver(typeRegistry: TypeRegistry, gn: GraphNode): Option[ChildResolver] =
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
        // With dynamic nodes we can't know this is invalid. A TreeMaterializer backed path expression
        // that doesn't match and thus had no kids is a valid case and it won't be able to define types
        if (gn.hasTag(TreeNode.Dynamic)) {
          None
        }
        else {
          // Type is unknown. This is an error
          throw NoSuchTypeException
        }
    }

  private val eligibleNode: GraphNode => Boolean =
    n => n.hasTag(tag) || n.nodeName == tag

  // Attempt to find nodes of the required type under the given node
  private def findMeUnder(gn: GraphNode, ec: ExecutionContext, edgeName: Option[String]): Seq[GraphNode] = {
    // Filter for node name if we're following an edge
    val nameFilter: GraphNode => Boolean = edgeName match {
      case None | Some("*") => _ => true
      case Some(name) => n => n.nodeName == name
    }
    val r = gn.relatedNodes.filter(eligibleNode).filter(nameFilter) match {
      case Nil =>
        childResolver(ec.typeRegistry, gn) match {
          case Some(cr) => edgeName match {
            case None =>
              cr.findAllIn(gn).getOrElse(Nil)
            case Some(name) =>
              cr.navigate(gn, name, ec).getOrElse(Nil)
          }
          case None =>
            Nil
        }
      case kids =>
        kids
    }
    logger.debug(s"$this Returned $r for edgeName=$edgeName below $gn")
    r
  }

  override def follow(gn: GraphNode, axis: AxisSpecifier,
                      ee: ExpressionEngine,
                      executionContext: ExecutionContext): ExecutionResult = {
    try {
      axis match {
        case NavigationAxis(propertyName) =>
          // Follow edge or look for nodes with the name
          val nodes = gn.followEdge(propertyName).filter(eligibleNode)
          if (nodes.nonEmpty)
            ExecutionResult(nodes)
          else {
            ExecutionResult(gn.relatedNodesNamed(propertyName).filter(eligibleNode)) match {
              case Right(Nil) =>
                ExecutionResult(findMeUnder(gn, executionContext, Some(propertyName)))
              case l => l
            }
          }
        case Self =>
          ExecutionResult(List(gn).filter(eligibleNode))
        case Child =>
          ExecutionResult(findMeUnder(gn, executionContext, None))
        case Descendant =>
          val allDescendants = Descendant.selfAndAllDescendants(gn)
          val found: Seq[GraphNode] = allDescendants.flatMap(d =>
            findMeUnder(d, executionContext, None))
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
