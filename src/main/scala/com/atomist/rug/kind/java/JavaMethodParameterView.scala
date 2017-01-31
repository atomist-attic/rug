package com.atomist.rug.kind.java

import com.atomist.rug.spi.{ExportFunction, TerminalView, ViewSupport}
import com.github.javaparser.ast.body.Parameter

class JavaMethodParameterView(originalBackingObject: Parameter, parent: JavaMethodView)
  extends ViewSupport[Parameter](originalBackingObject, parent)
    with TerminalView[Parameter] {
  
  override def nodeName: String = name

  override def childNodeNames: Set[String] = Set()

  override def tags: Set[String] = Set(JavaTypeType.MethodAlias)

  @ExportFunction(readOnly = true, description = "Return the name of the parameter")
  def name: String = currentBackingObject.getId.getName
}