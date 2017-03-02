package com.atomist.rug.kind.java

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.kind.java.support._
import com.atomist.rug.spi._

class JavaProjectType
  extends Type
    with ReflectivelyTypedType {

  override def description = "Java project"

  override def runtimeClass = classOf[JavaProjectMutableView]

  override def findAllIn(context: GraphNode): Option[Seq[MutableView[_]]] = {
    context match {
      case pv: ProjectMutableView if JavaAssertions.isJava(pv.currentBackingObject) =>
        Some(Seq(new JavaProjectMutableView(pv)))
      case _ => Some(Nil)
    }
  }
}