package com.atomist.rug.kind.rug.archive

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.runtime.rugdsl.DefaultEvaluator
import com.atomist.rug.spi.{ReflectivelyTypedType, Type}

class RugArchiveProjectType
  extends Type(DefaultEvaluator)
  with ReflectivelyTypedType {

  def runtimeClass = classOf[RugArchiveProjectMutableView]

  // Members declared in com.atomist.rug.spi.Typed
  def description: String = "Rug archive"

  // Members declared in com.atomist.rug.kind.dynamic.ViewFinder
  override def findAllIn(context: GraphNode): Option[Seq[com.atomist.rug.spi.MutableView[_]]] = {
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
