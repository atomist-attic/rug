package com.atomist.rug.kind.pom

import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.parser.Selected
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}
import com.atomist.source.ArtifactSource

/**
  * Maven POM type
  *
  * @param evaluator used to evaluate expressions
  */
class PomType(
               evaluator: Evaluator
             )
  extends Type(evaluator)
    with ReflectivelyTypedType {

  def this() = this(DefaultEvaluator)

  override def name = "pom"

  override def description = "POM XML file"

  override def viewManifest: Manifest[PomMutableView] = manifest[PomMutableView]

  override protected def findAllIn(rugAs: ArtifactSource, selected: Selected, context: MutableView[_],
                                   poa: ProjectOperationArguments,
                                   identifierMap: Map[String, Object]): Option[Seq[MutableView[_]]] = {
    context match {
      case pmv: ProjectMutableView =>
        Some(pmv.currentBackingObject.allFiles
          .filter(f => f.path.equals("pom.xml"))
          .map(f => new PomMutableView(f, pmv))
        )
      case _ => None
    }
  }
}
