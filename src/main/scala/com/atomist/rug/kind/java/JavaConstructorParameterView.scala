package com.atomist.rug.kind.java

import com.atomist.rug.spi.{ExportFunction, TerminalView, ViewSupport}
import com.github.javaparser.ast.body.Parameter

class JavaConstructorParameterView(originalBackingObject: Parameter, parent: JavaConstructorMutableView)
  extends ViewSupport[Parameter](originalBackingObject, parent)
    with TerminalView[Parameter] {

  override def nodeName: String = "constructor"

  override def nodeTags: Set[String] = Set("constructor")

  override def childNodeNames: Set[String] = Set()

  @ExportFunction(readOnly = true, description = "Return the name of the parameter")
  def name: String = currentBackingObject.getNameAsString

}