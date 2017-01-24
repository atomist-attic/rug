package com.atomist.rug.kind.yml

import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.kind.dynamic.ContextlessViewFinder
import com.atomist.rug.parser.Selected
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}
import com.atomist.source.ArtifactSource
import com.atomist.tree.TreeNode

class YmlType(
               evaluator: Evaluator
             )
  extends Type(evaluator)
    with ContextlessViewFinder
    with ReflectivelyTypedType {

  def this() = this(DefaultEvaluator)

  override def description = "YML file"

  override def viewManifest: Manifest[YmlMutableView] = manifest[YmlMutableView]

  //  override protected def findInternal(typeName: String, creationInfo: Option[Object], rugAs: ArtifactSource, projectSource: ArtifactSource, poa: ProjectOperationArguments, identifierMap: Map[String, Object], targetAlias: String): Seq[YmlMutableView] = {
  //    val pmv = new ProjectMutableView(rugAs, projectSource)
  //    projectSource.allFiles
  //      .filter(f => f.name.endsWith(".yml"))
  //      .map(f => new YmlMutableView(f, new ProjectMutableView(rugAs, projectSource)))
  //      .toSeq
  //  }
  override protected def findAllIn(rugAs: ArtifactSource,
                                   selected: Selected,
                                   context: TreeNode,
                                   poa: ProjectOperationArguments,
                                   identifierMap: Map[String, Object]): Option[Seq[MutableView[_]]] = ???

  /**
    * The set of node types this can resolve from
    *
    * @return set of node types this can resolve from
    */
  override val resolvesFromNodeTypes: Set[String] = Set("project", "directory", "file")
}
