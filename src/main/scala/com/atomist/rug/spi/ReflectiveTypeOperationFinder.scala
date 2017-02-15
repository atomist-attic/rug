package com.atomist.rug.spi

/**
  * Uses reflection to export type information about a type.
  * Types with methods with @ExportFunction annotations should
  * return this type, passing their own class as argument.
  *
  * @param classToExamine class to look for annotated operations on
  * @see ExportFunction
  */
class ReflectiveTypeOperationFinder(classToExamine: Class[_]) {

  val operations: Seq[TypeOperation] = {
    ReflectiveFunctionExport.exportedOperations(classToExamine)
      .sortWith((a, b) => a.name < b.name)
  }
}

trait ReflectivelyTypedType extends Type {

  /**
    * Expose type information. Return an instance of StaticTypeInformation if
    * operations are known to help with compile time validation and tooling.
    *
    * @return type information.
    */
  final override val operations: Seq[TypeOperation] =
    new ReflectiveTypeOperationFinder(runtimeClass).operations
}

/**
  * Extended by classes that can describe existing types that aren't exposed to
  * top level navigation
  * @param c class to expose
  */
abstract class TypeProvider(c: Class[_]) extends Typed {

  override val name: String = Typed.typeToTypeName(c)

  override def operations: Seq[TypeOperation] =
    new ReflectiveTypeOperationFinder(c).operations
}
