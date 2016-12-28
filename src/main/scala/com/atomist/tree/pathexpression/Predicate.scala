package com.atomist.tree.pathexpression

import com.atomist.tree.{ContainerTreeNode, TreeNode}

/**
  * Based on the XPath concept of a predicate. A predicate acts on a sequence of nodes
  * returned from navigation to filter them.
  * Predicates can be evaluated against materialized objects, and most predicates expose enough information
  * to generate queries against external systems to retrieve data.
  */
trait Predicate {

  def name: String

  /**
    * Function taking nodes returned by navigation
    * to filter them. We test one node with knowledge of all returned nodes.
    *
    * @param nodeToTest    node we're testing on;
    * @param returnedNodes all nodes returned. This argument is
    *                      often ignored, but can be used to discern the index of the target node.
    */
  def evaluate(nodeToTest: TreeNode, returnedNodes: Seq[TreeNode]): Boolean

  def and(that: Predicate): Predicate =
    AndPredicate(this, that)

  def or(that: Predicate): Predicate =
    OrPredicate(this, that)

  def not: Predicate =
    NegationOfPredicate(this)

}


case object TruePredicate extends Predicate {

  override def name: String = "true"

  override def evaluate(root: TreeNode, returnedNodes: Seq[TreeNode]): Boolean = true
}


case object FalsePredicate extends Predicate {

  override def name: String = "false"

  override def evaluate(root: TreeNode, returnedNodes: Seq[TreeNode]): Boolean = false
}


case class NegationOfPredicate(p: Predicate) extends Predicate {

  override def name: String = "!" + p.name

  override def evaluate(root: TreeNode, returnedNodes: Seq[TreeNode]): Boolean = !p.evaluate(root, returnedNodes)
}

case class AndPredicate(a: Predicate, b: Predicate) extends Predicate {

  override def name: String = a.name + " and " + b.name

  override def evaluate(root: TreeNode, returnedNodes: Seq[TreeNode]): Boolean =
    a.evaluate(root, returnedNodes) && b.evaluate(root, returnedNodes)
}

case class OrPredicate(a: Predicate, b: Predicate) extends Predicate {

  override def name: String = a.name + " or " + b.name

  override def evaluate(root: TreeNode, returnedNodes: Seq[TreeNode]): Boolean =
    a.evaluate(root, returnedNodes) || b.evaluate(root, returnedNodes)
}

/**
  * Test for the index of the given node among all returned nodes.
  * XPath indexes from 1, and unfortunately we need to do that also.
  */
case class IndexPredicate(name: String, i: Int) extends Predicate {

  def evaluate(tn: TreeNode, among: Seq[TreeNode]): Boolean = {
    val index = among.indexOf(tn)
    if (index == -1)
      throw new IllegalStateException(s"Internal error: Index [$i] not found in collection $among")
    i == index + 1
  }

}

case class PropertyValueTest(property: String, expectedValue: String) extends Predicate {

  override def name: String = s"$property=[$expectedValue]"

  override def evaluate(n: TreeNode, returnedNodes: Seq[TreeNode]): Boolean =
      n match {
        case ctn: ContainerTreeNode =>
          val extracted = ctn.childrenNamed(property)
          if (extracted.size == 1) {
            extracted.head.value.equals(expectedValue)
          }
          else
            false
        case _ => false
      }
}

case class NodeNameTest(expectedName: String) extends Predicate {

  override def name: String = s"name=[$expectedName]"

  override def evaluate(n: TreeNode, returnedNodes: Seq[TreeNode]): Boolean =
    n.nodeName.equals(expectedName)
}


case class NodeTypeTest(expectedType: String) extends Predicate {

  override def name: String = s"type=[$expectedType]"

  override def evaluate(n: TreeNode, returnedNodes: Seq[TreeNode]): Boolean =
    n.nodeType.equals(expectedType)

}


/**
  * Needs to run against JVM objects. Can't be used to
  * generate queries, but needs to be executed against materialized tree
  *
  * @param name name of the predicate
  * @param f    function to run against returned classes
  */
case class FunctionPredicate(name: String, f: (TreeNode, Seq[TreeNode]) => Boolean) extends Predicate {

  def evaluate(tn: TreeNode, among: Seq[TreeNode]): Boolean = f(tn, among)

}