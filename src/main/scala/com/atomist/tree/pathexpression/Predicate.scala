package com.atomist.tree.pathexpression

import com.atomist.rug.spi.TypeRegistry
import com.atomist.tree.pathexpression.ExpressionEngine.NodePreparer
import com.atomist.tree.{ContainerTreeNode, TreeNode}
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
  def evaluate(nodeToTest: TreeNode,
               returnedNodes: Seq[TreeNode],
               ee: ExpressionEngine,
               typeRegistry: TypeRegistry,
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

  override def evaluate(root: TreeNode,
                        returnedNodes: Seq[TreeNode],
                        ee: ExpressionEngine,
                        typeRegistry: TypeRegistry,
                        nodePreparer: Option[NodePreparer]): Boolean = true
}

case object FalsePredicate extends Predicate {

  override def toString: String = "false"

  override def evaluate(root: TreeNode,
                        returnedNodes: Seq[TreeNode],
                        ee: ExpressionEngine,
                        typeRegistry: TypeRegistry,
                        nodePreparer: Option[NodePreparer]): Boolean = false
}

case class NegationOfPredicate(p: Predicate) extends Predicate {

  override def toString: String = "!" + p.name

  override def evaluate(root: TreeNode,
                        returnedNodes: Seq[TreeNode],
                        ee: ExpressionEngine,
                        typeRegistry: TypeRegistry,
                        nodePreparer: Option[NodePreparer]): Boolean = !p.evaluate(root, returnedNodes, ee, typeRegistry, nodePreparer)
}

case class AndPredicate(a: Predicate, b: Predicate) extends Predicate {

  override def toString: String = a.name + " and " + b.name

  override def evaluate(root: TreeNode,
                        returnedNodes: Seq[TreeNode],
                        ee: ExpressionEngine,
                        typeRegistry: TypeRegistry,
                        nodePreparer: Option[NodePreparer]): Boolean =
    a.evaluate(root, returnedNodes, ee, typeRegistry, nodePreparer) &&
      b.evaluate(root, returnedNodes, ee, typeRegistry, nodePreparer)
}

case class OrPredicate(a: Predicate, b: Predicate) extends Predicate {

  override def toString: String = a.name + " or " + b.name

  override def evaluate(root: TreeNode,
                        returnedNodes: Seq[TreeNode],
                        ee: ExpressionEngine,
                        typeRegistry: TypeRegistry,
                        nodePreparer: Option[NodePreparer]): Boolean =
    a.evaluate(root, returnedNodes, ee, typeRegistry, nodePreparer) ||
      b.evaluate(root, returnedNodes, ee, typeRegistry, nodePreparer)
}

/**
  * Predicate that may or may not exist.  This is typically used with
  * NestedPathExpressionPredicates so they get materialized in the returned
  * node tree.
  *
  * @param optionalPredicate
  */
case class OptionalPredicate(optionalPredicate: Predicate) extends Predicate {
  override def toString: String = optionalPredicate.name + "?"

  /**
    * Always returns true.
    *
    * @param root
    * @param returnedNodes all nodes returned. This argument is
    *                      often ignored, but can be used to discern the index of the target node.
    * @param ee
    * @param typeRegistry
    * @param nodePreparer
    * @return
    */
  override def evaluate(root: TreeNode,
                        returnedNodes: Seq[TreeNode],
                        ee: ExpressionEngine,
                        typeRegistry: TypeRegistry,
                        nodePreparer: Option[NodePreparer]): Boolean = true
}

/**
  * Test for the index of the given node among all returned nodes.
  * XPath indexes from 1, and unfortunately we need to do that also.
  */
case class IndexPredicate(i: Int) extends Predicate {

  def evaluate(tn: TreeNode,
               among: Seq[TreeNode],
               ee: ExpressionEngine,
               typeRegistry: TypeRegistry,
               nodePreparer: Option[NodePreparer]): Boolean = {
    val index = among.indexOf(tn)
    if (index == -1)
      throw new IllegalStateException(s"Internal error: Index [$i] not found in collection $among")
    i == index + 1
  }

}

case class PropertyValuePredicate(property: String, expectedValue: String) extends Predicate {

  override def toString: String = s"@$property=$expectedValue"

  override def evaluate(n: TreeNode,
                        returnedNodes: Seq[TreeNode],
                        ee: ExpressionEngine,
                        typeRegistry: TypeRegistry,
                        nodePreparer: Option[NodePreparer]): Boolean = {
    if (property == "value") {
      // Treat the value property specially
      n.value == expectedValue
    }
    else {
      val extracted = n.childrenNamed(property)
      if (extracted.size == 1) {
        val result = extracted.head.value.equals(expectedValue)
        //println(s"Comparing property [$property] of [${extracted.head.value}] against expected [$expectedValue] gave $result")
        result
      }
      else {
        // TODO try to invoke a method?
        false
      }
    }
  }
}

case class NodeNamePredicate(expectedName: String) extends Predicate {

  override def toString: String = s"@name=$expectedName"

  override def evaluate(n: TreeNode,
                        returnedNodes: Seq[TreeNode],
                        ee: ExpressionEngine,
                        typeRegistry: TypeRegistry,
                        nodePreparer: Option[NodePreparer]): Boolean =
    n.nodeName.equals(expectedName)
}

case class NodeTypePredicate(expectedType: String) extends Predicate {

  override def toString: String = s"@type=$expectedType"

  override def evaluate(n: TreeNode,
                        returnedNodes: Seq[TreeNode],
                        ee: ExpressionEngine,
                        typeRegistry: TypeRegistry,
                        nodePreparer: Option[NodePreparer]): Boolean =
    n.nodeType.contains(expectedType)

}

/**
  * Needs to run against JVM objects. Can't be used to
  * generate queries, but needs to be executed against materialized tree
  *
  * @param name name of the predicate
  * @param f    function to run against returned classes
  */
case class FunctionPredicate(override val name: String, f: (TreeNode, Seq[TreeNode]) => Boolean)
  extends Predicate {

  def evaluate(tn: TreeNode,
               among: Seq[TreeNode],
               ee: ExpressionEngine,
               typeRegistry: TypeRegistry,
               nodePreparer: Option[NodePreparer]): Boolean = f(tn, among)

}

case class NestedPathExpressionPredicate(expression: PathExpression) extends Predicate {

  override def evaluate(nodeToTest: TreeNode,
                        returnedNodes: Seq[TreeNode],
                        ee: ExpressionEngine,
                        typeRegistry: TypeRegistry,
                        nodePreparer: Option[NodePreparer]): Boolean = {
    ee.evaluate(nodeToTest, expression, typeRegistry, nodePreparer) match {
      case Left(_) => false
      case Right(nodes) => nodes.nonEmpty
    }
  }

}
