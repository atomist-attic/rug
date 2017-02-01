package com.atomist.rug.kind.yml

import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.kind.core.{DirectoryMutableView, FileMutableView, ProjectMutableView}
import com.atomist.rug.kind.dynamic.ContextlessViewFinder
import com.atomist.rug.parser.Selected
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}
import com.atomist.source.ArtifactSource
import com.atomist.tree.TreeNode

object YmlType {
  val ymlExtension = ".yml"
}

class YmlType(
               evaluator: Evaluator
             )
  extends Type(evaluator)
    with ContextlessViewFinder
    with ReflectivelyTypedType {

  import YmlType._

  def this() = this(DefaultEvaluator)

  override def description =
    """
      |YAML file.  If the file contains multiple YAML documents, only the first is parsed and addressable.
    """.stripMargin

  override def viewManifest: Manifest[YmlMutableView] = manifest[YmlMutableView]

  override protected def findAllIn(rugAs: ArtifactSource,
                                   selected: Selected,
                                   context: TreeNode,
                                   poa: ProjectOperationArguments,
                                   identifierMap: Map[String, Object]): Option[Seq[MutableView[_]]] = {
    context match {
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

  /**
    * The set of node types this can resolve from
    *
    * @return set of node types this can resolve from
    */
  override val resolvesFromNodeTypes: Set[String] = Set("project", "directory", "file")
}
