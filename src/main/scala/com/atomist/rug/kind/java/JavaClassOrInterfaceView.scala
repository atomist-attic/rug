package com.atomist.rug.kind.java

import com.atomist.rug.RugRuntimeException
import com.atomist.rug.kind.java.JavaClassType._
import com.atomist.rug.spi._
import com.github.javaparser.ast.body._

import scala.collection.JavaConversions._

class JavaClassOrInterfaceView(old: ClassOrInterfaceDeclaration, parent: JavaSourceMutableView)
  extends TypeDeclarationView[ClassOrInterfaceDeclaration](old, parent) {

  override def nodeName: String = currentBackingObject.getName

  override def nodeType: String = JavaTypeAlias

  override def childrenNames: Seq[String] = Seq(ConstructorAlias, MethodAlias, FieldAlias)

  override def children(fieldName: String): Seq[MutableView[_]] = fieldName match {
    case ConstructorAlias =>
      currentBackingObject.getMembers
        .collect {
          case c: ConstructorDeclaration => c
        }.map(c => new JavaConstructorView(c, this))
    case MethodAlias =>
      currentBackingObject.getMembers
        .collect {
          case m: MethodDeclaration => m
        }.map(m => new JavaMethodView(m, this))
    case FieldAlias =>
      currentBackingObject.getMembers
        .collect {
          case f: FieldDeclaration => f
        }.map(m => new JavaFieldView(m, this))
    case _ =>
      throw new RugRuntimeException(null, s"No child with name '$fieldName' in ${getClass.getSimpleName}")
  }

  @ExportFunction(readOnly = true, description = "Is this an interface?")
  def isInterface: Boolean = currentBackingObject.isInterface

  @ExportFunction(readOnly = true, description = "Is this abstract?")
  def isAbstract: Boolean =
    isInterface || ModifierSet.isAbstract(currentBackingObject.getModifiers)

  @ExportFunction(readOnly = true, description = "Does this type extend the given type?")
  def inheritsFrom(
                    @ExportFunctionParameterDescription(name = "simpleName",
                      description = "Simple name of the ancestor class we're looking for")
                    simpleName: String): Boolean =
    currentBackingObject.getExtends.exists(t => t.getName.equals(simpleName))
}
