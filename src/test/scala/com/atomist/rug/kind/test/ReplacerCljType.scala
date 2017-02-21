package com.atomist.rug.kind.test

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi._
import com.atomist.tree.TreeNode

class ReplacerCljType(ev: Evaluator)
  extends Type(ev) with ReflectivelyTypedType {

  def this() = this(DefaultEvaluator)

  def description = "Test type for replacing the content of clojure files"

  override def runtimeClass: Class[StringReplacingMutableView] = classOf[StringReplacingMutableView]

  protected def listViews(context: GraphNode): Seq[MutableView[_]] = context match {
    case pmv: ProjectMutableView =>
      pmv.currentBackingObject.allFiles.filter(f => f.path.contains(".clj"))
        .map(f => new StringReplacingMutableView(f, pmv))
    case _ => Nil
  }

  override def findAllIn(context: GraphNode): Option[Seq[MutableView[_]]] = Option(listViews(context))
}
