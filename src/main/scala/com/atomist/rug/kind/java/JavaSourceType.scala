package com.atomist.rug.kind.java

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.core.{FileArtifactBackedMutableView, ProjectMutableView}
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi._
import com.typesafe.scalalogging.LazyLogging

/**
  * Represents a Java source file.
  *
  * @param evaluator evaluator
  */
class JavaSourceType(evaluator: Evaluator)
  extends Type(evaluator)
    with ReflectivelyTypedType
    with LazyLogging {

  def this() = this(DefaultEvaluator)

  override def description = "Java source file"

  override def runtimeClass = classOf[JavaSourceMutableView]

  override def findAllIn(context: GraphNode): Option[Seq[MutableView[_]]] = context match {
      case pv: ProjectMutableView =>
        Some(JavaProjectMutableView(pv).javaSourceViews)
      case fmv: FileArtifactBackedMutableView =>
        val jpv = JavaProjectMutableView(fmv.parent)
        Some(Seq(new JavaSourceMutableView(fmv.currentBackingObject, jpv)))
      case _ => None
    }
}

object JavaSourceType {

  val JavaExtension = ".java"

  val FieldAlias = "field"
}
