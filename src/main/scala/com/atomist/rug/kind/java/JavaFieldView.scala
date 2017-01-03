package com.atomist.rug.kind.java

import com.atomist.rug.spi.{ExportFunction, TerminalView}
import com.github.javaparser.ast.body.FieldDeclaration

import scala.collection.JavaConverters._

class JavaFieldView(originalBackingObject: FieldDeclaration, parent: JavaClassOrInterfaceView)
  extends BodyDeclarationView[FieldDeclaration](originalBackingObject, parent)
    with TerminalView[FieldDeclaration] {

  override def nodeName: String = name

  override def nodeType: Set[String] = Set(JavaSourceType.FieldAlias)

  override def childNodeNames: Set[String] = Set()

  @ExportFunction(readOnly = true, description = "Return the name of the field")
  def name: String = {
    if (currentBackingObject.getVariables.size != 1)
      throw new UnsupportedOperationException(s"Can only handle 1 variable declaration: $currentBackingObject")
    currentBackingObject.getVariables.asScala.head.getId.getName
  }
}