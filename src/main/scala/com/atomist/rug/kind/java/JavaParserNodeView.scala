package com.atomist.rug.kind.java

import com.atomist.rug.kind.core.FileMetrics
import com.atomist.rug.spi.{MutableView, ViewSupport}
import com.github.javaparser.ast.Node

abstract class JavaParserNodeView[T <: Node](originalBackingObject: T, parent: MutableView[_])
  extends ViewSupport[T](originalBackingObject, parent)
    with FileMetrics {

}
