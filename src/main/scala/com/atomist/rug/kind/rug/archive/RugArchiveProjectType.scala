package com.atomist.rug.kind.rug.archive

import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.runtime.rugdsl.DefaultEvaluator
import com.atomist.rug.spi.{ReflectivelyTypedType, Type}
import com.atomist.tree.TreeNode

class RugArchiveProjectType
  extends Type(DefaultEvaluator)
  with ReflectivelyTypedType {
  def viewManifest: Manifest[_] = manifest[RugArchiveProjectMutableView]

  // Members declared in com.atomist.rug.spi.Typed
  def description: String = "Rug archive"

  // Members declared in com.atomist.rug.kind.dynamic.ViewFinder
  protected def findAllIn(rugAs: com.atomist.source.ArtifactSource,
                          selected: com.atomist.rug.parser.Selected,
                          context: TreeNode,
                          poa: com.atomist.project.ProjectOperationArguments,
                          identifierMap: Map[String,Object]): Option[Seq[com.atomist.rug.spi.MutableView[_]]] = {
    context match {
      case pv: ProjectMutableView if pv.directoryExists(".atomist") =>
        Some(Seq(new RugArchiveProjectMutableView(pv)))
      case _ => Some(Nil)
    }
  }

}

object RugArchiveProjectType {
  val RugExtension = ".rug"
  val TypeScriptExtension = ".ts"
}