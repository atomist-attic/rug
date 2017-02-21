package com.atomist.rug.kind.docker

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi.{ReflectivelyTypedType, Type}
import com.atomist.tree.TreeNode

class DockerFileType(
                  evaluator: Evaluator
                )
  extends Type(evaluator)
    with ReflectivelyTypedType {

  def this() = this(DefaultEvaluator)

  import DockerFileType._

  def description: String = "Docker file type"

  override def runtimeClass = classOf[DockerFileMutableView]

  override def findAllIn(context: GraphNode): Option[Seq[TreeNode]] = context match {
      case pmv: ProjectMutableView =>
        Some(pmv.currentBackingObject
          .allFiles
          .filter(f => f.name == DockerFileName)
          .map(f => new DockerFileMutableView(f, pmv))
        )
      case _ => None
    }
}

object DockerFileType {

  val DockerFileName: String = "Dockerfile"

}
