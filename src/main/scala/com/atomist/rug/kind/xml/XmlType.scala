package com.atomist.rug.kind.xml

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.core.{FileMutableView, ProjectMutableView}
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}

class XmlType
  extends Type
    with ReflectivelyTypedType {

  import XmlType._

  override def description = "XML"

  override def runtimeClass = classOf[XmlMutableView]

  override def findAllIn(context: GraphNode): Option[Seq[MutableView[_]]] = {
    context match {
      case pmv: ProjectMutableView =>
        Some(pmv.currentBackingObject.allFiles
          .filter(f => f.name.endsWith(xmlExt))
          .map(f => new XmlMutableView(f, pmv))
        )
      case fmv: FileMutableView if fmv.name.endsWith(xmlExt) =>
        Some(Seq(new XmlMutableView(fmv.currentBackingObject, fmv.parent)))
      case _ => None
    }
  }
}

object XmlType {
  val xmlExt = ".xml"
}
