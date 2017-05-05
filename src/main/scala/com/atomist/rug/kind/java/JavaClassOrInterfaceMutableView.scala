package com.atomist.rug.kind.java

import com.atomist.rug.kind.java.JavaTypeType._
import com.atomist.rug.spi._
import com.github.javaparser.ast.body._

import scala.collection.JavaConverters._

class JavaClassOrInterfaceMutableView(old: ClassOrInterfaceDeclaration, parent: JavaSourceMutableView)
  extends TypeDeclarationView[ClassOrInterfaceDeclaration](old, parent) {

  override def nodeName: String = currentBackingObject.getName

  override def nodeTags: Set[String] = Set(JavaTypeAlias)

  override def childNodeNames: Set[String] = Set(ConstructorAlias, MethodAlias, FieldAlias)

  override def childrenNamed(fieldName: String): Seq[MutableView[_]] = fieldName match {
    case ConstructorAlias =>
      currentBackingObject.getMembers.asScala
        .collect {
          case c: ConstructorDeclaration =>
            new JavaConstructorMutableView(c, this)
        }
    case MethodAlias =>
      currentBackingObject.getMembers.asScala
        .collect {
          case m: MethodDeclaration => new JavaMethodMutableView(m, this)
        }
    case FieldAlias =>
      currentBackingObject.getMembers.asScala
        .collect {
          case f: FieldDeclaration => new JavaFieldMutableView(f, this)
        }
    case _ =>
      Nil
  }

  @ExportFunction(readOnly = true,
    exposeAsProperty = true,
    description = "Is this an interface?")
  def sourceFile: JavaSourceMutableView = parent

  @ExportFunction(readOnly = true,
    exposeAsProperty = true,
    description = "Is this an interface?")
  def isInterface: Boolean = currentBackingObject.isInterface

  @ExportFunction(readOnly = true,
    exposeAsProperty = true,
    description = "Is this type abstract?")
  def isAbstract: Boolean =
    isInterface || ModifierSet.isAbstract(currentBackingObject.getModifiers)

  @ExportFunction(readOnly = true, description = "Does this type extend the given type?")
  def inheritsFrom(
                    @ExportFunctionParameterDescription(name = "simpleName",
                      description = "Simple name of the ancestor class we're looking for")
                    simpleName: String): Boolean =
    currentBackingObject.getExtends.asScala.exists(_.getName.equals(simpleName))
}
