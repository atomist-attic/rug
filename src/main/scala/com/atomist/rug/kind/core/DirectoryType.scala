package com.atomist.rug.kind.core

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.dynamic.ChildResolver
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}

class DirectoryType
  extends Type
  with ReflectivelyTypedType
  with ChildResolver {

  override def description = "Type for a directory within a project."

  override def runtimeClass = classOf[DirectoryMutableView]

  override def findAllIn(context: GraphNode): Option[Seq[MutableView[_]]] = context match {
      case pmv: ProjectMutableView =>
        Some(pmv.currentBackingObject.allDirectories.map(DirectoryMutableView(_, pmv)))
      case _ => None
    }
}
