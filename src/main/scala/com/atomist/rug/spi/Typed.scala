package com.atomist.rug.spi

import com.atomist.util.lang.JavaHelpers

object Typed {

  def typeClassToTypeName(tc: Class[_]): String = tc.getSimpleName match {
    case n if n.endsWith("Type") => n.dropRight(4)
    case n => n
  }

  def typeClassesToTypeNames(tcs: Class[_]*): Set[String] =
    tcs.map(tc => typeClassToTypeName(tc)).toSet

  def typeToTypeName(tc: Class[_], searchable: Boolean = true): String = {
    val raw = tc.getSimpleName match {
      case n if n.endsWith("TreeNode") => n.dropRight("TreeNode".length)
      case n if n.endsWith("MutableView") => n.dropRight("MutableView".length)
      case n => n
    }
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
    * Description of this type
    */
  def description: String

  /**
    * FQN of the type
    * @return
    */
  def underlyingType: Class[_]

  /**
    * Expose type information. Return an instance of StaticTypeInformation if
    * operations are known to help with compile time validation and tooling.
    * @return type information.
    */
  def typeInformation: TypeInformation
}
