package com.atomist.rug.kind.pom

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.core.{FileArtifactBackedMutableView, ProjectMutableView}
import com.atomist.rug.kind.dynamic.ChildResolver
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}

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

  override def runtimeClass = classOf[PomMutableView]

  override def findAllIn(context: GraphNode): Option[Seq[MutableView[_]]] = context match {
      case pmv: ProjectMutableView =>
        pmv.currentBackingObject.findFile("pom.xml")
          .map(f => Seq(new PomMutableView(f, pmv))).orElse(Some(Seq()))
      case f: FileArtifactBackedMutableView if f.filename == "pom.xml" =>
        Some(Seq(new PomMutableView(f.currentBackingObject, f.parent)))
      case _ => None
    }
}
