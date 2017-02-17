package com.atomist.rug.kind.core

import com.atomist.graph.GraphNode
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi.{ReflectivelyTypedType, Type}
import com.atomist.tree.TreeNode

class ProjectType(
                   evaluator: Evaluator
                 )
  extends Type(evaluator)
    with ReflectivelyTypedType {

  def this() = this(DefaultEvaluator)

  override def description: String = "Type for a project. Supports global operations. " +
    "Consider using file and other lower types by preference as project" +
    "operations can be inefficient."

  override def runtimeClass = classOf[ProjectMutableView]

  override def findAllIn(context: GraphNode): Option[Seq[TreeNode]] = {
    // Special case where we want only one
    context match {
      case pmv: ProjectMutableView =>
        Some(Seq(pmv))
      case _ => None
    }
  }
}

object ProjectType {

  /**
    * File containing provenance information in root of edited projects
    */
  val ProvenanceFilePath = ".atomist.yml"
}
