package com.atomist.rug.kind.java

import com.atomist.rug.spi.{ExportFunction, ExportFunctionParameterDescription}
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.comments.BlockComment

abstract class TypeDeclarationView[T <: TypeDeclaration[T]](originalBackingObject: T, parent: JavaSourceMutableView)
  extends BodyDeclarationView[T](originalBackingObject, parent) {

  def compilationUnit: Option[CompilationUnit] = parent.compilationUnit

  @ExportFunction(readOnly = true, exposeAsProperty = true, description = "Return the package")
  def pkg: String = parent.pkg

  @ExportFunction(readOnly = true, exposeAsProperty = true, description = "Return the name of the type")
  def name: String = currentBackingObject.getNameAsString

  @ExportFunction(readOnly = false, description = "Add or replace header comment for this type")
  def setHeaderComment(@ExportFunctionParameterDescription(name = "comment",
                          description = "New header comment to set")
                        comment: String): Unit = {
    currentBackingObject.setComment(new BlockComment(comment))
  }

  @ExportFunction(readOnly = false, description = "Move the type to the given package")
  def movePackage(@ExportFunctionParameterDescription(name = "newPackage",
    description = "The package to move the type to")
                  newPackage: String): Unit = {
    parent.movePackage(newPackage)
  }

  @ExportFunction(readOnly = false, description = "Rename the type")
  def rename(@ExportFunctionParameterDescription(name = "newName",
    description = "The new name of the type")
             newName: String): Unit = {
    currentBackingObject.setName(newName)
    parent.rename(newName + ".java")
  }

  @ExportFunction(readOnly = false, description = "Rename the type by replacing a pattern in the name")
  def renameByReplace(@ExportFunctionParameterDescription(name = "target",
    description = "The name of the type to replace")
                      target: String,
                      @ExportFunctionParameterDescription(name = "replacement",
                        description = "The replacement pattern")
                      replacement: String): Unit = {
    val currentName = currentBackingObject.getNameAsString
    val newName = currentName.replaceAll(target, replacement)
    currentBackingObject.setName(newName)
    parent.rename(newName + ".java")
  }
}
