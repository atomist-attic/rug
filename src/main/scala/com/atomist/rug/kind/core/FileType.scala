package com.atomist.rug.kind.core

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.dynamic.ChildResolver
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi.{ReflectivelyTypedType, Type}
import com.atomist.tree.TreeNode

class FileType(
                evaluator: Evaluator
              )
  extends Type(evaluator)
    with ReflectivelyTypedType
    with ChildResolver {

  def this() = this(DefaultEvaluator)

  override def description = "Type for a file within a project."

  override def runtimeClass = classOf[FileMutableView]

  override def findAllIn(context: GraphNode): Option[Seq[TreeNode]] = context match {
    case pmv: ProjectMutableView =>
      Some(pmv.currentBackingObject.allFiles.map(f => new FileMutableView(f, pmv)))
    case _ => None
  }
}
