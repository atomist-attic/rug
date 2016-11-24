package com.atomist.rug.kind.properties

import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.kind.core.{FileArtifactMutableView, ProjectMutableView}
import com.atomist.rug.parser.Selected
import com.atomist.rug.runtime.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}
import com.atomist.source.ArtifactSource

class PropertiesType(
                      evaluator: Evaluator
                    )
  extends Type(evaluator)
    with ReflectivelyTypedType {

  def this() = this(DefaultEvaluator)

  override def name = "properties"

  override def description = "Properties file"

  override def viewManifest: Manifest[PropertiesMutableView] = manifest[PropertiesMutableView]

  override protected def findAllIn(rugAs: ArtifactSource, selected: Selected,
                                   context: MutableView[_],
                                   poa: ProjectOperationArguments,
                                   identifierMap: Map[String, Object]): Option[Seq[MutableView[_]]] = {
    context match {
      case fmv: FileArtifactMutableView =>
        Some(Seq(fmv.originalBackingObject)
          .filter(f => f.name.endsWith(".properties"))
          .map(f => new PropertiesMutableView(f, fmv.parent)))
      case pmv: ProjectMutableView =>
        Some(pmv.originalBackingObject.allFiles
          .filter(f => f.name.endsWith(".properties"))
          .map(f => new PropertiesMutableView(f, pmv)))
    }
  }
}
