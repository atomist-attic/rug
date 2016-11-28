package com.atomist.model.content.text

object Selector {

  type Selector = (TreeNode, Seq[TreeNode]) => Boolean
}

import Selector.Selector

case class Predicate(name: String, f: Selector) extends Selector {

  override def apply(n: TreeNode, among: Seq[TreeNode]) = f(n, among)

  def and(that: Predicate): Predicate =
    Predicate(s"${this.name} and ${that.name}", (n, among) => this(n, among) && that(n, among))

  def or(predicate: Predicate): Predicate = ???

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
