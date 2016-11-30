package com.atomist.rug.spi

/**
  * Extended by language elements to return as much type information as
  * possible to help with compile time validation and tooling.
  */
trait Typed {

  /**
    * Name for use in Rug scripts. e.g "file" in "with file f"
    *
    * @return alias for use in Rug scripts
    */
  def name: String

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
