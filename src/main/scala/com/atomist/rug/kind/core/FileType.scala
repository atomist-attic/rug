package com.atomist.rug.kind.core

import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.kind.dynamic.ChildResolver
import com.atomist.rug.parser.Selected
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}
import com.atomist.source.ArtifactSource
import com.atomist.tree.TreeNode

class FileType(
                evaluator: Evaluator
              )
  extends Type(evaluator)
    with ReflectivelyTypedType
    with ChildResolver {

  def this() = this(DefaultEvaluator)

  override def description = "Type for a file within a project."

  override def viewManifest: Manifest[FileMutableView] = manifest[FileMutableView]

  override type Self = this.type

  override def findAllIn(context: TreeNode): Option[Seq[TreeNode]] = context match {
    case pmv: ProjectMutableView =>
      Some(pmv.currentBackingObject.allFiles.map(f => new FileMutableView(f, pmv)))
    case _ => None
  }
}
