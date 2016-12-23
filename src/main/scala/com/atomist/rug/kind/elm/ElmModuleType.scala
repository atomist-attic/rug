package com.atomist.rug.kind.elm

import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.kind.core.{DirectoryMutableView, FileArtifactBackedMutableView, ProjectMutableView}
import com.atomist.rug.kind.dynamic.ContextlessViewFinder
import com.atomist.rug.parser.Selected
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}
import com.atomist.source.ArtifactSource

class ElmModuleType(
                     evaluator: Evaluator
                   )
  extends Type(evaluator)
    with ReflectivelyTypedType
    with ContextlessViewFinder {

  import ElmModuleType._

  def this() = this(DefaultEvaluator)

  override val resolvesFromNodeTypes: Set[String] = Set("project", "file", "directory")

  override def description = "Elm module"

  override def viewManifest: Manifest[ElmModuleMutableView] = manifest[ElmModuleMutableView]

  override protected def findAllIn(rugAs: ArtifactSource,
                                   selected: Selected,
                                   context: MutableView[_],
                                   poa: ProjectOperationArguments,
                                   identifierMap: Map[String, Object]): Option[Seq[MutableView[_]]] = {
    context match {
      case pmv: ProjectMutableView =>
        val elmp = new ElmProjectMutableView(pmv)
        Some(pmv.currentBackingObject
          .allFiles
          .filter(f => f.name.endsWith(ElmExtension))
          .map(f => new ElmModuleMutableView(f, elmp)))
      case f: FileArtifactBackedMutableView =>
        val elmp = new ElmProjectMutableView(f.parent)
        if (f.filename.endsWith(ElmExtension) && !f.path.contains("elm-stuff"))
          Some(Seq(new ElmModuleMutableView(f.currentBackingObject, elmp)))
        else
          Some(Nil)
      case d: DirectoryMutableView =>
        val elmp = new ElmProjectMutableView(d.parent)
        Some(elmp.currentBackingObject
          .allFiles
          .filter(f => f.name.endsWith(ElmExtension))
          .map(f => new ElmModuleMutableView(f, elmp)))
      case _ => None
    }
  }
}

/**
  * Contains names of Elm types for navigation within Rug scripts
  */
object ElmModuleType {

  val ElmExtension = ".elm"

  val FunctionAlias = "function"

  val TypeAlias = "type"

  val TypeAliasAlias = "type.alias"

  val ImportAlias = "import"

  val RecordTypeAlias = "recordType"

  val RecordValueAlias = "recordValue"

  val CaseAlias = "case"

  val CaseClauseAlias = "caseClause"
}
