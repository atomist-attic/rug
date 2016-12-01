package com.atomist.rug.kind.js

import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.parser.Selected
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}
import com.atomist.source.ArtifactSource

class PackageJsonType(
                       evaluator: Evaluator
                     )
  extends Type(evaluator)
    with ReflectivelyTypedType {

  def this() = this(DefaultEvaluator)

  override def name = "packageJSON"

  override def description = "package.json configuration file"

  override def viewManifest: Manifest[PackageJsonMutableView] = manifest[PackageJsonMutableView]

  override protected def findAllIn(rugAs: ArtifactSource, selected: Selected, context: MutableView[_],
                                   poa: ProjectOperationArguments,
                                   identifierMap: Map[String, Object]): Option[Seq[MutableView[_]]] = {
    context match {
      case pmv: ProjectMutableView =>
        Some(pmv.currentBackingObject
          .findFile("package.json")
          .map(f => new PackageJsonMutableView(f, pmv))
          .toSeq
        )
      case _ => None
    }
  }
}
