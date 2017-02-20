package com.atomist.rug.kind.pom

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}

/**
  * Maven POM type
  *
  * @param evaluator used to evaluate expressions
  */
class EveryPomType(
               evaluator: Evaluator
             )
  extends Type(evaluator)
    with ReflectivelyTypedType {

  def this() = this(DefaultEvaluator)

  override def description = "POM XML file"

  override def runtimeClass = classOf[PomMutableView]

  override def findAllIn(context: GraphNode): Option[Seq[MutableView[_]]] = {
    context match {
      case pmv: ProjectMutableView =>
        Some(pmv.currentBackingObject.allFiles
          .filter(f => f.name.equals("pom.xml"))
          .map(f => new PomMutableView(f, pmv))
        )
      case _ => None
    }
  }
}
