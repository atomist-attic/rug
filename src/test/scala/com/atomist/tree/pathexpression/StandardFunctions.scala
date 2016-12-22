package com.atomist.tree.pathexpression

/**
  * Well known functions that can be used in path expressions.
  * Analogous to the XPath core function library.
  * The execution engine reflectively dispatches to these functions,
  * so simply adding more will result in an enhanced library.
  */
object StandardFunctions {

  // TODO null checks
  def substring(s: String, n: Int) = s.substring(n)

  def contains(s: String, a: String) = s.contains(a)
}
