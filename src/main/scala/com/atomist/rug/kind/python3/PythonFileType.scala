package com.atomist.rug.kind.python3

import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.kind.core.{FileMutableView, ProjectMutableView}
import com.atomist.rug.kind.dynamic.{ContextlessViewFinder, MutableContainerMutableView}
import com.atomist.rug.parser.Selected
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi._
import com.atomist.source.{ArtifactSource, FileArtifact}
import com.atomist.tree.TreeNode

/**
  * Contains aliases for navigation down simplified AST.
  */
object PythonFileType {

  val PythonExtension = ".py"

  val pythonParser = new Python3Parser

  val ImportAlias = "import"

  val PythonFileAlias = "python"

  /**
    * Requirements at well-known location
    */
  val RequirementsTextTypeAlias = "python.requirements.txt"

  /** Path within archive of Python requirements.txt */
  val RequirementsTextPath = "requirements.txt"

  val RequirementAlias = "requirement"
}

import com.atomist.rug.kind.python3.PythonFileType._

class PythonFileType(
                      evaluator: Evaluator
                    )
  extends Type(evaluator)
    with ReflectivelyTypedType
    with ContextlessViewFinder {

  def this() = this(DefaultEvaluator)

  override def description = "Python file"

  override def resolvesFromNodeTypes = Set("File")

  override def viewManifest: Manifest[MutableContainerMutableView] = manifest[MutableContainerMutableView]

  override protected def findAllIn(rugAs: ArtifactSource,
                                   selected: Selected,
                                   context: TreeNode,
                                   poa: ProjectOperationArguments,
                                   identifierMap: Map[String, Object]): Option[Seq[TreeNode]] = {
    context match {
      case pmv: ProjectMutableView =>
        Some(pmv.currentBackingObject
          .files
          .filter(f => f.name.endsWith(PythonExtension))
          .map(f => toView(f, pmv))
        )
      case f: FileMutableView if f.filename.endsWith(PythonExtension) =>
        Some(Seq(toView(f.currentBackingObject, f.parent)))
      case _ => None
    }
  }

  private def toView(f: FileArtifact, pmv: ProjectMutableView): TreeNode = {
    new PythonFileMutableView(f, pmv)
  }
}
