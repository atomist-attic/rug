package com.atomist.tree.pathexpression

/**
  * Core supported types. Inspired by XPath spec.
  */
object XPathTypes extends Enumeration {

  type XPathType = Value

  val String, Boolean = Value
}
