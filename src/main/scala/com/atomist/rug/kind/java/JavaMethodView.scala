package com.atomist.rug.kind.java

import com.atomist.rug.RugRuntimeException
import com.atomist.rug.spi._
import com.github.javaparser.ast.body.MethodDeclaration

import scala.collection.JavaConverters._

class JavaMethodView(originalBackingObject: MethodDeclaration, parent: JavaClassOrInterfaceView)
  extends BodyDeclarationView[MethodDeclaration](originalBackingObject, parent) {

  override def nodeName: String = name

  override def tags: Set[String] = Set(JavaTypeType.MethodAlias)

  override def childNodeNames: Set[String] = Set("JavaParameter")

  override def childrenNamed(fieldName: String): Seq[MutableView[_]] = fieldName match {
    case "JavaParameter" =>
      currentBackingObject.getParameters.asScala.map(new JavaMethodParameterView(_, this))
    case _ => throw new RugRuntimeException(null, s"No child with name '$fieldName' in ${getClass.getSimpleName}")
  }

  @ExportFunction(readOnly = true, description = "Return the name of the method")
  def name: String = currentBackingObject.getName

  @ExportFunction(readOnly = true,
    description = "Return the Javadoc for the method, or an empty string if there isn't any")
  def javadoc(): String =
    DocumentableNodeUtils.javadoc(currentBackingObject)
}
