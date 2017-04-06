package com.atomist.rug.kind.java

import com.atomist.rug.spi.{ExportFunction, TerminalView, TypeProvider}
import com.github.javaparser.ast.body.FieldDeclaration

import scala.collection.JavaConverters._

class JavaFieldTypeProvider extends TypeProvider(classOf[JavaFieldMutableView]) {

  override def description: String = "Java field"
}

class JavaFieldMutableView(originalBackingObject: FieldDeclaration, parent: JavaClassOrInterfaceMutableView)
  extends BodyDeclarationView[FieldDeclaration](originalBackingObject, parent)
    with TerminalView[FieldDeclaration] {

  override def nodeName: String = name

  override def nodeTags: Set[String] = Set(JavaSourceType.FieldAlias)

  override def childNodeNames: Set[String] = Set()

  @ExportFunction(readOnly = true,
    exposeAsProperty = true,
    description = "Type this is on")
  def `type`: JavaClassOrInterfaceMutableView = parent

  @ExportFunction(readOnly = true, description = "Return the name of the field")
  def name: String = {
    if (currentBackingObject.getVariables.size != 1)
      throw new UnsupportedOperationException(s"Can only handle 1 variable declaration: $currentBackingObject")
    currentBackingObject.getVariables.asScala.head.getId.getName
  }
}