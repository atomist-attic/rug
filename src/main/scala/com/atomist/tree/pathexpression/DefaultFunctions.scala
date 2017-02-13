package com.atomist.tree.pathexpression

/**
  * Defines default functions. Simply add functions here and type conversion will be automatic
  */
object DefaultFunctions {

  def contains(a: String, b: String): Boolean =
    a != null && b != null && a.contains(b)

}
