package com.atomist.rug.spi

/**
  * Uses reflection to export type information about a type.
  * Types with methods with @ExportFunction annotations should
  * return this type, passing their own class as argument.
  *
  * @param classToExamine
  * @see ExportFunction
  */
class ReflectiveStaticTypeInformation(classToExamine: Class[_]) extends StaticTypeInformation {

  override val operations: Seq[TypeOperation] = {
    ReflectiveFunctionExport.exportedOperations(classToExamine)
      .sortWith((a, b) => a.name < b.name)
  }
}

trait ReflectivelyTypedType extends Typed {

  type Self <: Type

  def self = this.asInstanceOf[Self]

  /**
    * Expose type information. Return an instance of StaticTypeInformation if
    * operations are known to help with compile time validation and tooling.
    *
    * @return type information.
    */
  final override val typeInformation: TypeInformation =
    new ReflectiveStaticTypeInformation(self.viewManifest.runtimeClass)
}
