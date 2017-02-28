package com.atomist.rug.kind.xml

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}

class XmlType
  extends Type
    with ReflectivelyTypedType {

  override def description = "XML"

  override def runtimeClass = classOf[XmlMutableView]

  override def findAllIn(context: GraphNode): Option[Seq[MutableView[_]]] = {
    context match {
      case pmv: ProjectMutableView =>
        Some(pmv.currentBackingObject.allFiles
          .filter(f => f.name.endsWith(".xml"))
          .map(f => new XmlMutableView(f, pmv))
        )
      case _ => None
    }
  }
}
