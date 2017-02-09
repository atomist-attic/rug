package com.atomist.rug.kind.js

import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}
import com.atomist.tree.TreeNode

class PackageJsonType(
                       evaluator: Evaluator
                     )
  extends Type(evaluator)
    with ReflectivelyTypedType {

  def this() = this(DefaultEvaluator)

  override def description = "package.json configuration file"

  override def runtimeClass = classOf[PackageJsonMutableView]

  override def findAllIn(context: TreeNode): Option[Seq[MutableView[_]]] = {
    context match {
      case pmv: ProjectMutableView =>
        Some(pmv.currentBackingObject
          .findFile("package.json")
          .map(f => new PackageJsonMutableView(f, pmv))
          .toSeq
        )
      case _ => None
    }
  }
}
