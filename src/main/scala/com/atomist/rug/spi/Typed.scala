package com.atomist.rug.spi

import com.atomist.util.lang.JavaHelpers

object Typed {

  private[spi] def trimSuffix(suffix: String, orig: String): String = orig match {
    case n if n.endsWith(suffix) => n.dropRight(suffix.size)
    case n => n
  }

  private val typeSuffix = "Type"
  private val treeNodeSuffix = "TreeNode"
  private val mutableViewSuffix = "MutableView"

  def typeClassToTypeName(tc: Class[_]): String = trimSuffix(typeSuffix, tc.getSimpleName)

  def typeClassesToTypeNames(tcs: Class[_]*): Set[String] = tcs.map(typeClassToTypeName).toSet

  def typeToTypeName(tc: Class[_], searchable: Boolean = true): String = {
    val raw = trimSuffix(treeNodeSuffix, trimSuffix(mutableViewSuffix, tc.getSimpleName))
    if (!searchable)
      JavaHelpers.lowerize(raw)
    else raw
  }
}

/**
  * Extended by language elements to return as much type information as
  * possible to help with compile time validation and tooling.
  */
trait Typed {

  /**
    * Name for use in Rug scripts. e.g "file" in "with File f"
    *
    * @return alias for use in Rug scripts
    */
  val name: String = Typed.typeClassToTypeName(getClass)

  /**
    * Description of this type.
    */
  def description: String

  /**
    * Expose type information. Return an instance of StaticTypeInformation if
    * operations are known to help with compile time validation and tooling.
    *
    * @return type information
    */
  def typeInformation: TypeInformation
}
