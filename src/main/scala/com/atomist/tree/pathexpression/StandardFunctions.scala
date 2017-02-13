package com.atomist.tree.pathexpression

/**
  * Defines default functions. Simply add functions here and type conversion will be automatic
  */
object StandardFunctions {

  def contains(a: String, b: String): Boolean =
    a != null && b != null && a.contains(b)

}
