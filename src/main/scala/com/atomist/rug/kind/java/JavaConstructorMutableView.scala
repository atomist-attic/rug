package com.atomist.rug.kind.java

import com.atomist.rug.RugRuntimeException
import com.atomist.rug.spi._
import com.github.javaparser.ast.body.ConstructorDeclaration

import scala.collection.JavaConverters._

class JavaConstructorTypeProvider extends TypeProvider(classOf[JavaConstructorMutableView]) {

  override def description: String = "Java constructor"
}

class JavaConstructorMutableView(originalBackingObject: ConstructorDeclaration, parent: JavaClassOrInterfaceMutableView)
  extends BodyDeclarationView[ConstructorDeclaration](originalBackingObject, parent) {

  override def nodeName: String = JavaTypeType.ConstructorAlias

  override def nodeTags: Set[String] = Set(JavaTypeType.ConstructorAlias)

  override def childNodeNames: Set[String] = Set("JavaParameter")

  override def childrenNamed(fieldName: String): Seq[MutableView[_]] = fieldName match {
    case "JavaParameter" =>
      currentBackingObject.getParameters.asScala.map(new JavaConstructorParameterView(_, this))
    case _ => throw new RugRuntimeException(null, s"No child with name '$fieldName' in ${getClass.getSimpleName}")
  }

  @ExportFunction(readOnly = true, description = "Return the name of the constructor")
  def name: String = currentBackingObject.getName

  @ExportFunction(readOnly = true,
    description = "Return the Javadoc for the constructor, or an empty string if there isn't any")
  def javadoc(): String =
    DocumentableNodeUtils.javadoc(currentBackingObject)

  @ExportFunction(readOnly = true, description = "Return the number of constructor parameters")
  def parametersSize: Int = currentBackingObject.getParameters.size

}
