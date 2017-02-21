package com.atomist.rug.kind.test

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi._
import org.springframework.beans.factory.annotation.Autowired

// Only used in tests
class ReplacerType(ev: Evaluator)
  extends Type(ev) with ReflectivelyTypedType {

  def this() = this(DefaultEvaluator)

  def runtimeClass = viewClass

  def description = "Test type for replacing the content of files"

  @Autowired
  protected def viewClass: Class[StringReplacingMutableView] = classOf[StringReplacingMutableView]

  protected def listViews(context: GraphNode): Seq[MutableView[_]] = context match {
    case pmv: ProjectMutableView =>
      pmv.currentBackingObject.allFiles.filter(f => f.path.contains(".java"))
        .map(f => new StringReplacingMutableView(f, pmv))
    case _ => Nil
  }

  def findAllIn(context: GraphNode): Option[Seq[MutableView[_]]] = Option(listViews(context))
}
