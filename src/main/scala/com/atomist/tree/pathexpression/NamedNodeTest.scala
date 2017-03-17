package com.atomist.tree.pathexpression

import java.util.Objects

import com.atomist.graph.GraphNode
import com.atomist.rug.spi.{TypeRegistry, Typed}
import com.atomist.tree.SimpleTerminalTreeNode
import com.atomist.tree.pathexpression.ExecutionResult.ExecutionResult
import com.atomist.tree.utils.NodeUtils

/**
  * Follow children of the given name. If not, methods of that name.
  *
  * @param name name of the node or relationship to try
  */
case class NamedNodeTest(name: String)
  extends NodeTest {

  private def findUnder(tn: GraphNode): List[GraphNode] = {
    tn.relatedNodesNamed(name).toList match {
      case Nil =>
        tn.followEdge(name).toList
      case l => l
    }
  }

  override def follow(tn: GraphNode, axis: AxisSpecifier, ee: ExpressionEngine, typeRegistry: TypeRegistry): ExecutionResult = axis match {
    case Child =>
      val kids: List[GraphNode] = findUnder(tn)
      ExecutionResult(kids)
    case Descendant =>
      val possibleMatches: List[GraphNode] = Descendant.selfAndAllDescendants(tn).flatMap(n => findUnder(n)).toList
      ExecutionResult(possibleMatches)
    case Attribute =>
      // If the property is not published, don't permit it
      val typed = Typed.typeFor(tn, typeRegistry)
      if (!typed.allOperations.exists(op => op.name == this.name && op.parameters.isEmpty))
        Left(s"No exported property [$name] on ${tn.nodeName}: Type info:[$typed]")
      else
        resultFromPropertyValue(tn, typeRegistry)
    case x => throw new UnsupportedOperationException(s"Unsupported axis $x in ${getClass.getSimpleName}")
  }

  /**
    * Invoke a method, which might return a GraphNode or a String or Int
    */
  private def resultFromPropertyValue(tn: GraphNode, typeRegistry: TypeRegistry): ExecutionResult = {
    import NodeUtils._

    if (hasNoArgMethod[GraphNode](tn, name)) {
      val n = invokeMethodIfPresent[GraphNode](tn, name)
      ExecutionResult(Seq(n.get))
    }
    else {
      // First look for a simple result
      val simpleProperty: Option[String] = () match {
        case _ if hasNoArgMethod[String](tn, name) =>
          invokeMethodIfPresent[String](tn, name)
        case _ if hasNoArgMethod[Int](tn, name) =>
          invokeMethodIfPresent[Int](tn, name).map(_.toString)
        case _ if hasNoArgMethod[Boolean](tn, name) =>
          invokeMethodIfPresent[Boolean](tn, name).map(_.toString)
        case _ => None
      }
      simpleProperty.map(value => {
        // Create a new graph node for the property
        ExecutionResult(Seq(SimpleTerminalTreeNode(name, value, Set("attribute"))))
      }).getOrElse(ExecutionResult.empty)
    }
  }

}
