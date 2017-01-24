package com.atomist.rug.kind.core

import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.kind.dynamic.ContextlessViewFinder
import com.atomist.rug.parser.Selected
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}
import com.atomist.source.ArtifactSource
import com.atomist.tree.TreeNode

class ProjectType(
                   evaluator: Evaluator
                 )
  extends Type(evaluator)
    with ReflectivelyTypedType
    with ContextlessViewFinder {

  def this() = this(DefaultEvaluator)

  override def description: String = "Type for a project. Supports global operations. " +
    "Consider using file and other lower types by preference as project" +
    "operations can be inefficient."

  override def viewManifest: Manifest[ProjectMutableView] = manifest[ProjectMutableView]

  override protected def findAllIn(rugAs: ArtifactSource,
                                   selected: Selected,
                                   context: TreeNode,
                                   poa: ProjectOperationArguments,
                                   identifierMap: Map[String, Object]): Option[Seq[TreeNode]] = {
    // Special case where we want only one
    context match {
      case pmv: ProjectMutableView =>
        Some(Seq(pmv))
      case _ => None
    }
  }

  override def resolvesFromNodeTypes: Set[String] = Set("project")
}

object ProjectType {

  /**
    * File containing provenance information in root of edited projects
    */
  val ProvenanceFilePath = ".atomist.yml"
}