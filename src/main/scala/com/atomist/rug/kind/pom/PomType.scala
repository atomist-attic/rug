package com.atomist.rug.kind.pom

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.core.{FileArtifactBackedMutableView, ProjectMutableView}
import com.atomist.rug.kind.dynamic.ChildResolver
import com.atomist.rug.spi.{MutableView, ReflectivelyTypedType, Type}

/**
  * Maven POM type
  */
class PomType
  extends Type
    with ReflectivelyTypedType
    with ChildResolver {

  import PomType.Filename

  override def description = "POM XML file"

  override def runtimeClass: Class[PomMutableView] = classOf[PomMutableView]

  override def findAllIn(context: GraphNode): Option[Seq[MutableView[_]]] = context match {
    case pmv: ProjectMutableView =>
      Some(pmv.currentBackingObject.findFile(Filename)
        .map(f => new PomMutableView(f, pmv)).toSeq)
    case f: FileArtifactBackedMutableView if f.filename == Filename =>
      Some(Seq(new PomMutableView(f.currentBackingObject, f.parent)))
    case _ => None
  }
}

object PomType {

  val Filename = "pom.xml"
}
