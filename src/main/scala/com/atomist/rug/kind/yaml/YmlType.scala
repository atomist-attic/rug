package com.atomist.rug.kind.yaml

import com.atomist.rug.kind.core._
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}
import com.atomist.tree.TreeNode

object YmlType {

  val yamlExtension = ".yml"
}

/**
  * @deprecated Use [[YamlType]] instead
  */
class YmlType(
               evaluator: Evaluator
             )
  extends Type(evaluator)
    with ReflectivelyTypedType {

  import YmlType._

  def this() = this(DefaultEvaluator)

  override def description = "YAML file.  If the file contains multiple YAML documents, only the first is parsed and addressable."

  override def runtimeClass = classOf[YamlMutableView]

  override def findAllIn(context: TreeNode): Option[Seq[MutableView[_]]] = context match {
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
