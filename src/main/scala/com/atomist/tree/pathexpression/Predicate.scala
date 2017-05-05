package com.atomist.tree.pathexpression

import com.atomist.graph.GraphNode
import com.atomist.rug.runtime.js.ExecutionContext
import com.atomist.rug.spi.TypeRegistry
import com.atomist.tree.TreeNode
import com.atomist.tree.pathexpression.ExpressionEngine.NodePreparer
import com.atomist.tree.utils.NodeUtils
import com.fasterxml.jackson.annotation.JsonProperty

/**
  * Based on the XPath concept of a predicate. A predicate acts on a sequence of nodes
  * returned from navigation to filter them.
  * Predicates can be evaluated against materialized objects, and most predicates expose enough information
  * to generate queries against external systems to retrieve data.
  */
trait Predicate {

  @JsonProperty
  def name: String = getClass.getSimpleName.replace("$", "")

  /**
    * Function taking nodes returned by navigation
    * to filter them. We test one node with knowledge of all returned nodes.
    *
    * @param nodeToTest    node we're testing on;
    * @param returnedNodes all nodes returned. This argument is
    *                      often ignored, but can be used to discern the index of the target node.
    */
  def evaluate(nodeToTest: GraphNode,
               returnedNodes: Seq[GraphNode],
               ee: ExpressionEngine,
               executionContext: ExecutionContext,
               nodePreparer: Option[NodePreparer]): Boolean

  def and(that: Predicate): Predicate =
    AndPredicate(this, that)

  def or(that: Predicate): Predicate =
    OrPredicate(this, that)

  def not: Predicate =
    NegationOfPredicate(this)

}

case object TruePredicate extends Predicate {

  override def toString: String = "true"

  override def evaluate(root: GraphNode,
                        returnedNodes: Seq[GraphNode],
                        ee: ExpressionEngine,
                        executionContext: ExecutionContext,
                        nodePreparer: Option[NodePreparer]): Boolean = true
}

case object FalsePredicate extends Predicate {

  override def toString: String = "false"

  override def evaluate(root: GraphNode,
                        returnedNodes: Seq[GraphNode],
                        ee: ExpressionEngine,
                        executionContext: ExecutionContext,
                        nodePreparer: Option[NodePreparer]): Boolean = false
}

case class NegationOfPredicate(p: Predicate) extends Predicate {

  override def toString: String = "!" + p.name

  override def evaluate(root: GraphNode,
                        returnedNodes: Seq[GraphNode],
                        ee: ExpressionEngine,
                        executionContext: ExecutionContext,
                        nodePreparer: Option[NodePreparer]): Boolean =
      !p.evaluate(root, returnedNodes,ee, executionContext, nodePreparer)
}

case class AndPredicate(a: Predicate, b: Predicate) extends Predicate {

  override def toString: String = a.name + " and " + b.name

  override def evaluate(root: GraphNode,
                        returnedNodes: Seq[GraphNode],
                        ee: ExpressionEngine,
                        executionContext: ExecutionContext,
                        nodePreparer: Option[NodePreparer]): Boolean =
    a.evaluate(root, returnedNodes, ee, executionContext, nodePreparer) &&
      b.evaluate(root, returnedNodes, ee, executionContext, nodePreparer)
}

case class OrPredicate(a: Predicate, b: Predicate) extends Predicate {

  override def toString: String = a.name + " or " + b.name

  override def evaluate(root: GraphNode,
                        returnedNodes: Seq[GraphNode],
                        ee: ExpressionEngine,
                        executionContext: ExecutionContext,
                        nodePreparer: Option[NodePreparer]): Boolean =
    a.evaluate(root, returnedNodes, ee, executionContext, nodePreparer) ||
      b.evaluate(root, returnedNodes, ee, executionContext, nodePreparer)
}

/**
  * Predicate that may or may not exist.  This is typically used with
  * NestedPathExpressionPredicates so they get materialized in the returned
  * node tree.
  */
case class OptionalPredicate(optionalPredicate: Predicate) extends Predicate {
  override def toString: String = optionalPredicate.name + "?"

  /**
    * Always returns true.
    *
    * @param returnedNodes all nodes returned. This argument is
    *                      often ignored, but can be used to discern the index of the target node.
    * @return
    */
  override def evaluate(root: GraphNode,
                        returnedNodes: Seq[GraphNode],
                        ee: ExpressionEngine,
                        executionContext: ExecutionContext,
                        nodePreparer: Option[NodePreparer]): Boolean = true
}

/**
  * Test for the index of the given node among all returned nodes.
  * XPath indexes from 1, and unfortunately we need to do that also.
  */
case class IndexPredicate(i: Int) extends Predicate {

  def evaluate(tn: GraphNode,
               among: Seq[GraphNode],
               ee: ExpressionEngine,
               executionContext: ExecutionContext,
               nodePreparer: Option[NodePreparer]): Boolean = {
    val index = among.indexOf(tn)
    if (index == -1)
      throw new IllegalStateException(s"Internal error: Index [$i] not found in collection $among")
    i == index + 1
  }

}

/**
  * Match the node name or a name property
  */
case class NodeNamePredicate(expectedName: String) extends Predicate {

  override def toString: String = s"@name=$expectedName"

  override def evaluate(n: GraphNode,
                        returnedNodes: Seq[GraphNode],
                        ee: ExpressionEngine,
                        executionContext: ExecutionContext,
                        nodePreparer: Option[NodePreparer]): Boolean =
    n.nodeName == expectedName || NodeUtils.keyValue(n, "name").contains(expectedName)
}

case class NodeTypePredicate(expectedType: String) extends Predicate {

  override def toString: String = s"@type=$expectedType"

  override def evaluate(n: GraphNode,
                        returnedNodes: Seq[GraphNode],
                        ee: ExpressionEngine,
                        executionContext: ExecutionContext,
                        nodePreparer: Option[NodePreparer]): Boolean =
    n.nodeTags.contains(expectedType)

}

/**
  * Needs to run against JVM objects. Can't be used to
  * generate queries, but needs to be executed against materialized tree
  *
  * @param name name of the predicate
  * @param f    function to run against returned classes
  */
case class FunctionPredicate(override val name: String, f: (GraphNode, Seq[GraphNode]) => Boolean)
  extends Predicate {

  def evaluate(tn: GraphNode,
               among: Seq[GraphNode],
               ee: ExpressionEngine,
               executionContext: ExecutionContext,
               nodePreparer: Option[NodePreparer]): Boolean = f(tn, among)

}

case class NestedPathExpressionPredicate(expression: PathExpression) extends Predicate {

  override def evaluate(nodeToTest: GraphNode,
                        returnedNodes: Seq[GraphNode],
                        ee: ExpressionEngine,
                        executionContext: ExecutionContext,
                        nodePreparer: Option[NodePreparer]): Boolean = {
    ee.evaluate(nodeToTest, expression, executionContext, nodePreparer) match {
      case Left(_) => false
      case Right(nodes) => nodes.nonEmpty
    }
  }

}
