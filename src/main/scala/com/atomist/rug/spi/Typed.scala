package com.atomist.rug.spi

import com.atomist.util.lang.JavaHelpers

object Typed {

  private[spi] def trimSuffix(suffix: String, orig: String): String =
    if (orig == suffix)
      orig
    else
      orig stripSuffix suffix

  private val TypeSuffix = "Type"
  private val TreeNodeSuffix = "TreeNode"
  private val MutableViewSuffix = "MutableView"

  def typeClassToTypeName(tc: Class[_]): String = trimSuffix(TypeSuffix, tc.getSimpleName)

  def typeClassesToTypeNames(tcs: Class[_]*): Set[String] = tcs.map(typeClassToTypeName).toSet

  def typeToTypeName(tc: Class[_], searchable: Boolean = true): String = {
    val raw = trimSuffix(TreeNodeSuffix, trimSuffix(MutableViewSuffix, tc.getSimpleName))
    if (searchable) raw
    else JavaHelpers.lowerize(raw)
  }
}

/**
  * Extended by language elements to return as much type information as
  * possible to help with compile time validation and tooling.
  */
trait Typed {

  /**
    * Name for use in Rug scripts. e.g "file" in "with File f".
    *
    * @return alias for use in Rug scripts
    */
  val name: String = Typed.typeClassToTypeName(getClass)

  /**
    * Description of this type.
    */
  def description: String

  /**
    * Operations on the type including its inheritance hierarchy.
    */
  def allOperations: Seq[TypeOperation]

  /**
    * Operations on the type.
    */
  def operations: Seq[TypeOperation]
}
