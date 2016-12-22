package com.atomist.tree.pathexpression

import com.atomist.tree.TreeNode

object Selector {

  /**
    * Function taking nodes returned by navigation
    * to filter them. First argument is the node we're testing on;
    * second argument is all nodes returned. The second argument is
    * often ignored, but can be used to discern the index of the target node.
    */
  type Selector = (TreeNode, Seq[TreeNode]) => Boolean
}

import com.atomist.tree.TreeNode
import com.atomist.tree.pathexpression.Selector.Selector

/**
  * Based on the XPath concept of a predicate. A predicate acts on a sequence of nodes
  * returned from navigation to filter them.
  */
trait Predicate extends Selector {

  def name: String

  def and(that: Predicate): Predicate =
    SimplePredicate(s"${this.name} and ${that.name}", (n, among) => this (n, among) && that(n, among))

  def or(that: Predicate): Predicate =
    SimplePredicate(s"${this.name} or ${that.name}", (n, among) => this (n, among) || that(n, among))

  def not(predicate: Predicate): Predicate = ???

}

/**
  * Convenient concrete predicate implementation
  */
case class SimplePredicate(name: String, f: Selector) extends Predicate {

  override def apply(n: TreeNode, among: Seq[TreeNode]): Boolean = f(n, among)

}


/**
  * Convenient support class for implementing predicates,
  * considering Scala's prohibition on case-case inheritance.
  */
abstract class AbstractPredicate(val name: String, f: Selector) extends Predicate {

  def apply(tn: TreeNode, among: Seq[TreeNode]): Boolean = f(tn, among)
}

case object TruePredicate extends AbstractPredicate("true", (_, _) => true)

case object FalsePredicate extends AbstractPredicate("false", (_, _) => false)

case class NegationOf(p: Predicate)
  extends AbstractPredicate(s"!${p.name}", (tn,among) => !p(tn, among))

/**
  * XPath indexes from 1, and unfortunately we need to do that also.
  */
case class IndexPredicate(name: String, i: Int) extends Predicate {

  def apply(tn: TreeNode, among: Seq[TreeNode]): Boolean = {
    val index = among.indexOf(tn)
    if (index == -1)
      throw new IllegalStateException(s"Internal error: Index [$i] not found in collection $among")
    i == index + 1
  }
}
