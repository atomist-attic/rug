package com.atomist.rug.kind.java

import com.atomist.rug.kind.core.FileMetrics
import com.atomist.rug.spi._
import com.atomist.util.lang.JavaParserUtils
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.BodyDeclaration

/**
  * Abstract class for views of fields, methods, and other body declarations backed by JavaParser.
  *
  * @tparam T underlying BodyDeclaration subclass
  */
abstract class BodyDeclarationView[T <: BodyDeclaration](originalBackingObject: T, parent: MutableView[_])
  extends ViewSupport[T](originalBackingObject, parent)
    with FileMetrics {

  final override def dirty = true

  override def childNodeTypes: Set[String] = childNodeNames

  @ExportFunction(readOnly = true, description = "Line count")
  override def lineCount: Int =
    FileMetrics.lineCount(currentBackingObject.toString)

  @ExportFunction(readOnly = false, description = "Add an import to the containing Java source")
  def addImport(@ExportFunctionParameterDescription(name = "fqn",
    description = "The fully qualified name of the import")
                fqn: String): Unit = {
    val compilationUnit: Option[CompilationUnit] = parent match {
      case jsv: JavaSourceMutableView => jsv.compilationUnit
      case jcoiv: JavaClassOrInterfaceMutableView => jcoiv.compilationUnit
      case _ => throw new IllegalArgumentException("Failed to get compilation unit")
    }
    compilationUnit.foreach(JavaParserUtils.addImportsIfNeeded(Seq(fqn), _))
  }

  @ExportFunction(readOnly = false, description = "Remove an import from the containing Java source")
  def removeImport(@ExportFunctionParameterDescription(name = "fqn",
    description = "The fully qualified name of the import")
                   fqn: String): Unit = {
    val compilationUnit: Option[CompilationUnit] = parent match {
      case jsv: JavaSourceMutableView => jsv.compilationUnit
      case jcoiv: JavaClassOrInterfaceMutableView => jcoiv.compilationUnit
      case _ => throw new IllegalArgumentException("Failed to get compilation unit")
    }
    compilationUnit.foreach(JavaParserUtils.removeImportsIfNeeded(Seq(fqn), _))
  }

  @ExportFunction(readOnly = true, description = "Does the element have the given annotation?")
  def hasAnnotation(@ExportFunctionParameterDescription(name = "annotation",
    description = "The string name of the annotation to look for")
                    annotation: String): Boolean = {
    JavaParserUtils.getAnnotation(currentBackingObject, annotation).isDefined
  }

  @ExportFunction(readOnly = false,
    description = "Annotate the element with the given annotation")
  def addAnnotation(@ExportFunctionParameterDescription(name = "pkg", description = "Package where the annotation is sourced")
                    pkg: String,
                    @ExportFunctionParameterDescription(name = "annotation",
                      description = "The annotation to add")
                    annotation: String): Unit = {
    JavaTypeType.annotationAddedTo(currentBackingObject, annotation)
    val annotationName: String =
      if (annotation.contains("(")) annotation.substring(0, annotation.indexOf('('))
      else annotation
    addImport(s"$pkg.$annotationName")
  }

  @ExportFunction(readOnly = false,
    description = "Remove annotation from the element")
  def removeAnnotation(@ExportFunctionParameterDescription(name = "pkg", description = "Package where the annotation is sourced")
                    pkg: String,
                    @ExportFunctionParameterDescription(name = "annotation",
                      description = "The annotation to remove")
                    annotation: String): Unit = {
    JavaTypeType.annotationRemovedFrom(currentBackingObject, annotation)
    // removeImport(s"$pkg.$annotation") TODO Check first if no other member is annotated with same annotation
  }

  /**
    * Commit all changes, invoking updaters and calling parent if necessary.
    */
  override def commit(): Unit = parent.commit()

  override def toString =
    s"JavaClassOrInterfaceView exposing $currentBackingObject: dirty=$dirty"
}
