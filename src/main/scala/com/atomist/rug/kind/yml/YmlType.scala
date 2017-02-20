package com.atomist.rug.kind.yml

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.core._
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}

object YmlType {

  val ymlExtension = ".yml"
}

class YmlType(
               evaluator: Evaluator
             )
  extends Type(evaluator)
    with ReflectivelyTypedType {

  import YmlType._

  def this() = this(DefaultEvaluator)

  override def description = "YAML file.  If the file contains multiple YAML documents, only the first is parsed and addressable."

  override def runtimeClass = classOf[YmlMutableView]

  override def findAllIn(context: GraphNode): Option[Seq[MutableView[_]]] = context match {
      case pmv: ProjectMutableView =>
        Some(pmv.originalBackingObject.allFiles
          .filter(f => f.name.endsWith(ymlExtension))
          .map(f => new YmlMutableView(f, pmv)))
      case dmv: DirectoryMutableView =>
        Some(dmv.originalBackingObject.allFiles
          .filter(f => f.name.endsWith(ymlExtension))
          .map(f => new YmlMutableView(f, dmv.parent)))
      case fmv: FileMutableView =>
        Some(Seq(fmv.originalBackingObject)
          .filter(f => f.name.endsWith(ymlExtension))
          .map(f => new YmlMutableView(f, fmv.parent)))
    }
}
