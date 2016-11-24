package com.atomist.rug.kind.java

import com.atomist.rug.kind.core.FileMetrics
import com.atomist.rug.kind.java.support.JavaParserUtils
import com.atomist.rug.spi._
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
  override def lineCount =
    FileMetrics.lineCount(currentBackingObject.toString)

  @ExportFunction(readOnly = false, description = "Add an import to the containing Java source")
  def addImport(@ExportFunctionParameterDescription(name = "fqn",
    description = "The fully qualified name of the import")
                fqn: String): Unit = {
    val compilationUnit: Option[CompilationUnit] = parent match {
      case jsv: JavaSourceMutableView => jsv.compilationUnit
      case jcoiv: JavaClassOrInterfaceView => jcoiv.compilationUnit
      case _ => throw new IllegalArgumentException("Failed to get compilation unit")
    }
    compilationUnit.foreach(cu => JavaParserProjectEditor.addImportsIfNeeded(Seq(fqn), cu))
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
    JavaClassType.annotationAddedTo(currentBackingObject, annotation)
    addImport(s"$pkg.$annotation")
  }

  /**
    * Commit all changes, invoking updaters and calling parent if necessary.
    */
  override def commit(): Unit = parent.commit()

  override def toString =
    s"JavaClassOrInterfaceView exposing $currentBackingObject: dirty=$dirty"
}
