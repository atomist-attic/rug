package com.atomist.rug.spi

object Typed {

  // TODO can we make this more strongly typed
  def typeClassToTypeName(tc: Class[_]): String = tc.getSimpleName match {
    case n if n.endsWith("Type") => n.dropRight(4)
    case n => n
  }

  def typeClassesToTypeNames(tcs: Class[_]*): Set[String] =
    tcs.map(tc => typeClassToTypeName(tc)).toSet

  def typeToTypeName(tc: Class[_]): String = tc.getSimpleName match {
    case n if n.endsWith("TreeNode") => n.dropRight("TreeNode".size)
    case n if n.endsWith("MutableView") => n.dropRight("MutableView".size)
    case n => n
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
