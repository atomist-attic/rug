package com.atomist.rug.kind.elm

import com.atomist.rug.kind.core._
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}
import com.atomist.tree.TreeNode

class ElmModuleType(
                     evaluator: Evaluator
                   )
  extends Type(evaluator)
    with ReflectivelyTypedType {

  import ElmModuleType._

  def this() = this(DefaultEvaluator)

  override def description = "Elm module"

  override def viewManifest: Manifest[ElmModuleMutableView] = manifest[ElmModuleMutableView]

  override def findAllIn(context: TreeNode): Option[Seq[MutableView[_]]] = context match {
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
