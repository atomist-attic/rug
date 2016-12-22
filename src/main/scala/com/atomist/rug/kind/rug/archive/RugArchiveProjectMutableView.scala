package com.atomist.rug.kind.rug.archive

import com.atomist.rug.kind.core.{FileArtifactBackedMutableView, ProjectMutableView}
import com.atomist.rug.kind.rug.dsl.{RugMutableView, RugType}
import com.atomist.rug.kind.support.ProjectDecoratingMutableView
import com.atomist.rug.spi.{MutableView, Typed}

import scala.collection.JavaConverters._

class RugArchiveProjectMutableView(pmv: ProjectMutableView)
  extends ProjectDecoratingMutableView(pmv) {

  val RugTypeName = Typed.typeToTypeName(classOf[RugMutableView])
  override def childrenNames: Seq[String] = RugTypeName +: super.childrenNames

  override def children(typeName: String): Seq[MutableView[_]] = typeName match {
    case RugTypeName =>
      pmv.files.asScala.
        filter(_.filename.endsWith(RugType.RugExtension)).
        map{fabmv: FileArtifactBackedMutableView => new RugMutableView(fabmv.currentBackingObject, this)}

    case other =>
      super.children(typeName)
  }

}
