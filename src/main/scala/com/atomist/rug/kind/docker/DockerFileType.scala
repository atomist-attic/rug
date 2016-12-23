package com.atomist.rug.kind.docker

import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.kind.dynamic.ContextlessViewFinder
import com.atomist.rug.parser.Selected
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}
import com.atomist.source.ArtifactSource

import scala.collection.immutable.Map

class DockerFileType(
                  evaluator: Evaluator
                )
  extends Type(evaluator)
    with ContextlessViewFinder
    with ReflectivelyTypedType {

  def this() = this(DefaultEvaluator)

  import DockerFileType._

  override val resolvesFromNodeTypes: Set[String] = Set("file")

  def description: String = "Docker file type"

  override def viewManifest: Manifest[DockerMutableView] = manifest[DockerMutableView]

  override protected def findAllIn(rugAs: ArtifactSource,
                                   selected: Selected,
                                   context: MutableView[_],
                                   poa: ProjectOperationArguments,
                                   identifierMap: Map[String, Object]): Option[Seq[MutableView[_]]] = {
    context match {
      case pmv: ProjectMutableView =>
        Some(pmv.currentBackingObject
          .allFiles
          .filter(f => f.name == DockerFileName)
          .map(f => new DockerMutableView(f, pmv))
        )
      case _ => None
    }
  }
}

object DockerFileType {

  val DockerFileName: String = "Dockerfile"

}
