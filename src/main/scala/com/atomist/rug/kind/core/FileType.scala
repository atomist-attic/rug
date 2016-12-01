package com.atomist.rug.kind.core

import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.parser.Selected
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}
import com.atomist.source.ArtifactSource

class FileType(
                evaluator: Evaluator
              )
  extends Type(evaluator)
    with ReflectivelyTypedType {

  def this() = this(DefaultEvaluator)

  override val name = "file"

  override def description =
    """
      |Type for a file within a project. Supports generic options such as find and replace.
    """.stripMargin

  override def viewManifest: Manifest[FileArtifactMutableView] = manifest[FileArtifactMutableView]

  override protected def findAllIn(rugAs: ArtifactSource, selected: Selected, context: MutableView[_],
                                   poa: ProjectOperationArguments, identifierMap: Map[String, Object]): Option[Seq[MutableView[_]]] = {
    (selected.kind, context) match {
      case (`name`, pmv: ProjectMutableView) =>
        Some(pmv.currentBackingObject.allFiles.map(f => new FileArtifactMutableView(f, pmv)))
      case _ => None
    }
  }
}
