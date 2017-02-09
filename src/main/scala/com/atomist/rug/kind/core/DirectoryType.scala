package com.atomist.rug.kind.core

import com.atomist.rug.kind.dynamic.ChildResolver
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}
import com.atomist.tree.TreeNode

class DirectoryType(evaluator: Evaluator)
  extends Type(evaluator)
  with ReflectivelyTypedType
  with ChildResolver {

  def this() = this(DefaultEvaluator)

  override def description = "Type for a directory within a project."

  override def runtimeClass: Class[_] = classOf[DirectoryMutableView]

  override def findAllIn(context: TreeNode): Option[Seq[MutableView[_]]] = context match {
      case pmv: ProjectMutableView =>
        Some(pmv.currentBackingObject.allDirectories.map(d => new DirectoryMutableView(d, pmv)))
      case _ => None
    }
}
