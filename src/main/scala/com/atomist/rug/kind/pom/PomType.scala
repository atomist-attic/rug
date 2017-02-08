package com.atomist.rug.kind.pom

import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.kind.dynamic.ChildResolver
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}
import com.atomist.tree.TreeNode

/**
  * Maven POM type
  *
  * @param evaluator used to evaluate expressions
  */
class PomType(
               evaluator: Evaluator
             )
  extends Type(evaluator)
    with ReflectivelyTypedType
    with ChildResolver {

  def this() = this(DefaultEvaluator)

  override def description = "POM XML file"

  override def viewManifest: Manifest[PomMutableView] = manifest[PomMutableView]

  override def findAllIn(context: TreeNode): Option[Seq[MutableView[_]]] = context match {
      case pmv: ProjectMutableView =>
        Some(pmv.currentBackingObject.allFiles
          .filter(f => f.path.equals("pom.xml"))
          .map(f => new PomMutableView(f, pmv))
        )
      case _ => None
    }
}
