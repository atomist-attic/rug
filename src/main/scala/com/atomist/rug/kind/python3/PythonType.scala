package com.atomist.rug.kind.python3

import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.kind.dynamic.MutableContainerTreeNodeMutableView
import com.atomist.rug.parser.Selected
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi._
import com.atomist.source.{ArtifactSource, FileArtifact}

/**
  * Contains aliases for navigation down simplified AST.
  */
object PythonType {

  val PythonExtension = ".py"

  val pythonParser = new Python3Parser

  val ImportAlias = "import"

  val PythonFileAlias = "python"

  /**
    * Requirements at well-known location
    */
  val RequirementsTextTypeAlias = "python.requirements.txt"

  val RequirementsTypeAlias = "python.requirements"

  /** Path within archive of Python requirements.txt */
  val RequirementsTextPath = "requirements.txt"

  val RequirementAlias = "requirement"
}

import com.atomist.rug.kind.python3.PythonType._

class PythonType(
                  evaluator: Evaluator
                )
  extends Type(evaluator)
    with ReflectivelyTypedType {

  def this() = this(DefaultEvaluator)

  override def name = PythonFileAlias

  override def description = "Python file"

  override def viewManifest: Manifest[MutableContainerTreeNodeMutableView] = manifest[MutableContainerTreeNodeMutableView]

  override protected def findAllIn(rugAs: ArtifactSource, selected: Selected, context: MutableView[_],
                                   poa: ProjectOperationArguments,
                                   identifierMap: Map[String, Object]): Option[Seq[MutableView[_]]] = {
    context match {
      case pmv: ProjectMutableView =>
        Some(pmv.currentBackingObject
          .files
          .filter(f => f.name.endsWith(PythonExtension))
          .map(f => toView(f, pmv))
        )
      case _ => None
    }
  }

  private def toView(f: FileArtifact, pmv: ProjectMutableView): MutableView[_] = {
    new PythonFileMutableView(f, pmv)
  }
}
