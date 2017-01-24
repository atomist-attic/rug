package com.atomist.rug.kind.java

import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.kind.core.{FileArtifactBackedMutableView, FileType, ProjectMutableView, ProjectType}
import com.atomist.rug.kind.dynamic.ContextlessViewFinder
import com.atomist.rug.parser.Selected
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi._
import com.atomist.source.ArtifactSource
import com.atomist.tree.TreeNode
import com.typesafe.scalalogging.LazyLogging

/**
  * Represents a Java source file.
  *
  * @param evaluator evaluator
  */
class JavaSourceType(evaluator: Evaluator)
  extends Type(evaluator)
    with ContextlessViewFinder
    with ReflectivelyTypedType
    with LazyLogging {

  def this() = this(DefaultEvaluator)

  override def description = "Java source file"

  override def viewManifest: Manifest[JavaSourceMutableView] = manifest[JavaSourceMutableView]

  override val resolvesFromNodeTypes: Set[String] =
    Typed.typeClassesToTypeNames(classOf[ProjectType], classOf[FileType], classOf[JavaSourceType])

  override protected def findAllIn(rugAs: ArtifactSource,
                                   selected: Selected,
                                   context: TreeNode,
                                   poa: ProjectOperationArguments,
                                   identifierMap: Map[String, Object]): Option[Seq[MutableView[_]]] = {
    context match {
      case pv: ProjectMutableView =>
        Some(JavaProjectMutableView(pv).javaSourceViews)
      case fmv: FileArtifactBackedMutableView =>
        val jpv = JavaProjectMutableView(fmv.parent)
        Some(Seq(new JavaSourceMutableView(fmv.currentBackingObject, jpv)))
      case _ => None
    }
  }
}

object JavaSourceType {

  val JavaExtension = ".java"

  val FieldAlias = "field"
}
