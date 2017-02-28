package com.atomist.rug.kind.yaml

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.core._
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}

object YamlType {

  val yamlExtension = ".yml"
}

class YamlType
  extends Type
    with ReflectivelyTypedType {

  import YamlType._

  override def description = "YAML file.  If the file contains multiple YAML documents, only the first is parsed and addressable."

  override def runtimeClass = classOf[YamlMutableView]

  override def findAllIn(context: GraphNode): Option[Seq[MutableView[_]]] = context match {
    case pmv: ProjectMutableView =>
      Some(pmv.originalBackingObject.allFiles
        .filter(_.name.endsWith(yamlExtension))
        .map(new YamlMutableView(_, pmv)))
    case dmv: DirectoryMutableView =>
      Some(dmv.originalBackingObject.allFiles
        .filter(_.name.endsWith(yamlExtension))
        .map(new YamlMutableView(_, dmv.parent)))
    case fmv: FileMutableView =>
      Some(Seq(fmv.originalBackingObject)
        .filter(_.name.endsWith(yamlExtension))
        .map(new YamlMutableView(_, fmv.parent)))
  }
}
