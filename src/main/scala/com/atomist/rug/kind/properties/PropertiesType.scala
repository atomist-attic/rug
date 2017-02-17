package com.atomist.rug.kind.properties

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.core.{FileMutableView, ProjectMutableView}
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}

class PropertiesType(
                      evaluator: Evaluator
                    )
  extends Type(evaluator)
    with ReflectivelyTypedType {

  def this() = this(DefaultEvaluator)

  override def description = "Java properties file"

  override def runtimeClass = classOf[PropertiesMutableView]

  override def findAllIn(context: GraphNode): Option[Seq[MutableView[_]]] = {
    context match {
      case fmv: FileMutableView =>
        Some(Seq(fmv.originalBackingObject)
          .filter(f => f.name.endsWith(".properties"))
          .map(f => new PropertiesMutableView(f, fmv.parent)))
      case pmv: ProjectMutableView =>
        Some(pmv.originalBackingObject.allFiles
          .filter(f => f.name.endsWith(".properties"))
          .map(f => new PropertiesMutableView(f, pmv)))
    }
  }
}
