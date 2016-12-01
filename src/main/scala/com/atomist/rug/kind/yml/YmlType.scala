package com.atomist.rug.kind.yml

import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.parser.Selected
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}
import com.atomist.source.ArtifactSource

object YmlType {

  val TypeName = "yml"
}

class YmlType(
               evaluator: Evaluator
             )
  extends Type(evaluator)
    with ReflectivelyTypedType {

  def this() = this(DefaultEvaluator)

  override def name = YmlType.TypeName

  override def description = "YML file"

  override def viewManifest: Manifest[YmlMutableView] = manifest[YmlMutableView]

  //  override protected def findInternal(typeName: String, creationInfo: Option[Object], rugAs: ArtifactSource, projectSource: ArtifactSource, poa: ProjectOperationArguments, identifierMap: Map[String, Object], targetAlias: String): Seq[YmlMutableView] = {
  //    val pmv = new ProjectMutableView(rugAs, projectSource)
  //    projectSource.allFiles
  //      .filter(f => f.name.endsWith(".yml"))
  //      .map(f => new YmlMutableView(f, new ProjectMutableView(rugAs, projectSource)))
  //      .toSeq
  //  }
  override protected def findAllIn(rugAs: ArtifactSource, selected: Selected, context: MutableView[_], poa: ProjectOperationArguments, identifierMap: Map[String, Object]): Option[Seq[MutableView[_]]] = ???
}
