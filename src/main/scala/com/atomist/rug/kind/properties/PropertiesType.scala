package com.atomist.rug.kind.properties

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.core.{DirectoryMutableView, FileMutableView, ProjectMutableView}
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}

class PropertiesType
  extends Type
    with ReflectivelyTypedType {

  override def description = "Java properties file"

  override def runtimeClass: Class[PropertiesMutableView] = classOf[PropertiesMutableView]

  override def findAllIn(context: GraphNode): Option[Seq[MutableView[_]]] = {
    context match {
      case fmv: FileMutableView =>
        Some(Seq(fmv.originalBackingObject)
          .filter(f => f.name.endsWith(".properties"))
          .map(f => new PropertiesMutableView(f, fmv.parent)))
      case dmv: DirectoryMutableView =>
        Some(dmv.originalBackingObject.files
          .filter(f => f.name.endsWith(".properties"))
          .map(f => new PropertiesMutableView(f, dmv.parent)))
      case pmv: ProjectMutableView =>
        Some(pmv.originalBackingObject.allFiles
          .filter(f => f.name.endsWith(".properties"))
          .map(f => new PropertiesMutableView(f, pmv)))
    }
  }
}
