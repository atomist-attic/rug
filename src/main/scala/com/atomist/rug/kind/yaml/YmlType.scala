package com.atomist.rug.kind.yaml

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.core._
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}

@deprecated("Use YamlType instead", "0.13.0")
class YmlType
  extends Type
    with ReflectivelyTypedType {

  import YamlType._

  override def description = "YAML file.  If the file contains multiple YAML documents, only the first is parsed and addressable."

  override def runtimeClass = classOf[YmlMutableView]

  override def findAllIn(context: GraphNode): Option[Seq[MutableView[_]]] = context match {
      case pmv: ProjectMutableView =>
        Some(pmv.originalBackingObject.allFiles
          .filter(f => f.name.endsWith(yamlExtension))
          .map(f => new YamlMutableView(f, pmv)))
      case dmv: DirectoryMutableView =>
        Some(dmv.originalBackingObject.allFiles
          .filter(f => f.name.endsWith(yamlExtension))
          .map(f => new YamlMutableView(f, dmv.parent)))
      case fmv: FileMutableView =>
        Some(Seq(fmv.originalBackingObject)
          .filter(f => f.name.endsWith(yamlExtension))
          .map(f => new YamlMutableView(f, fmv.parent)))
    }
}
