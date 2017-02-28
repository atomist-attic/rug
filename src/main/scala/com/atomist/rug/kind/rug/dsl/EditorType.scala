package com.atomist.rug.kind.rug.dsl

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.core.{DirectoryMutableView, FileArtifactBackedMutableView, ProjectMutableView}
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}

class EditorType extends Type with ReflectivelyTypedType {
  /** Describe the MutableView subclass to allow for reflective function export */

  override def runtimeClass = classOf[EditorMutableView]

  override def findAllIn(context: GraphNode): Option[Seq[MutableView[_]]] = context match {
      case pmv: ProjectMutableView => ???
      case f: FileArtifactBackedMutableView =>
        Some(Seq(new EditorMutableView(f.currentBackingObject, f.parent)))
      case d: DirectoryMutableView => ???
      case _ => None
    }

  /**
    * Description of this type
    */
  override def description: String = "Rug Editor, in Rug DSL or typescript"

}
