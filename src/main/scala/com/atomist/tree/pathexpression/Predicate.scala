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
  * @param name
  * @param f
  */
case class Predicate(name: String, f: Selector) extends Selector {

  override def apply(n: TreeNode, among: Seq[TreeNode]) = f(n, among)

  def and(that: Predicate): Predicate =
    Predicate(s"${this.name} and ${that.name}", (n, among) => this(n, among) && that(n, among))

  def or(that: Predicate): Predicate =
    Predicate(s"${this.name} or ${that.name}", (n, among) => this(n, among) || that(n, among))

  def not(predicate: Predicate): Predicate = ???

}

object TruePredicate extends Predicate("true", (_,_) => true)

object FalsePredicate extends Predicate("false", (_,_) => false)

class NegationOf(p: Predicate)
  extends Predicate(s"!${p.name}", (tn,among) => !p(tn, among))

/**
  * XPath indexes from 1, and unfortunately we need to do that also.
  */
class IndexPredicate(name: String, i: Int)
  extends Predicate(name, (tn, among) => {
    val index = among.indexOf(tn)
    if (index == -1)
      throw new IllegalStateException(s"Internal error: Index [$i] not found in collection $among")
    i == index + 1
  })
