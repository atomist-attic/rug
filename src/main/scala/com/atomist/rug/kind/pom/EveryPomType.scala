package com.atomist.rug.kind.pom

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}

/**
  * Maven POM type
  */
class EveryPomType
  extends Type
    with ReflectivelyTypedType {

  override def description = "POM XML file"

  override def runtimeClass = classOf[PomMutableView]

  override def findAllIn(context: GraphNode): Option[Seq[MutableView[_]]] = {
    context match {
      case pmv: ProjectMutableView =>
        Some(pmv.currentBackingObject.allFiles
          .filter(f => f.name.equals("pom.xml"))
          .map(f => new PomMutableView(f, pmv))
        )
      case _ => None
    }
  }
}
