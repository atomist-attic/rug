package com.atomist.rug.kind.java

import java.util.Objects

import com.atomist.rug.RugRuntimeException
import com.atomist.rug.kind.core.{LazyFileArtifactBackedMutableView, ProjectMutableView}
import com.atomist.rug.spi.{ExportFunction, ExportFunctionParameterDescription, MutableView}
import com.atomist.source.FileArtifact
import com.github.javaparser.JavaParser
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.{CompilationUnit, PackageDeclaration}

import scala.collection.JavaConverters._
import JavaTypeType._

import scala.util.Try

class JavaSourceMutableView(old: FileArtifact, parent: ProjectMutableView)
  extends LazyFileArtifactBackedMutableView(old, parent) {

  lazy val compilationUnit: Option[CompilationUnit] = {
    Try {
      // It's possible there'll be a parsing error. Hide it
      JavaParser.parse(old.inputStream)
    }.toOption
  }

  override protected def wellFormed: Boolean =
    compilationUnit.isDefined

  override def dirty = true

  override protected def currentContent: String = Objects.toString(compilationUnit.getOrElse(""))

  override def childNodeNames: Set[String] = Set(JavaTypeAlias)

  override def childrenNamed(fieldName: String): Seq[MutableView[_]] = (compilationUnit, fieldName) match {
    case (None, _) => Nil
    case (Some(cu), JavaTypeAlias) =>
      cu.getTypes.asScala
        .collect {
          case c: ClassOrInterfaceDeclaration => c
        }
        .map(c => new JavaClassOrInterfaceView(c, this))

    case _ => throw new RugRuntimeException(null, s"No child with name '$fieldName' in ${getClass.getSimpleName}")
  }

  override def commit(): Unit = {
    if (dirty) {
      val latest = currentBackingObject
      parent.updateFile(old, latest)
    }
  }

  /**
    * Return the package name.
    */
  @ExportFunction(readOnly = true, description = "Return the package name")
  def pkg: String = compilationUnit match {
    case Some(cu) => cu.getPackage.getPackageName
    case None => ""
  }

  @ExportFunction(readOnly = true, description = "Count the types in this source file")
  def typeCount: Int = compilationUnit match {
    case Some(cu) => cu.getTypes.size()
    case None => 0
  }

  @ExportFunction(readOnly = false, description = "Move the source file to the given package")
  def movePackage(@ExportFunctionParameterDescription(name = "newPackage",
    description = "The package to move the source file to")
                  newPackage: String): Unit = compilationUnit match {
    case Some(cu) => val pathToReplace = pkg.replace(".", "/")
      val newPath = newPackage.replace(".", "/")
      cu.setPackage(new PackageDeclaration(new NameExpr(newPackage)))
      setPath(path.replace(pathToReplace, newPath))
    case None =>
  }

  def rename(newName: String): Unit = {
    setName(newName)
  }
}