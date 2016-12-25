package com.atomist.rug.kind.rug.archive

import com.atomist.rug.kind.core.{FileArtifactBackedMutableView, ProjectMutableView}
import com.atomist.rug.kind.rug.dsl.EditorMutableView
import com.atomist.rug.kind.support.ProjectDecoratingMutableView
import com.atomist.rug.spi.{MutableView, Typed}

import scala.collection.JavaConverters._

class RugArchiveProjectMutableView(pmv: ProjectMutableView)
  extends ProjectDecoratingMutableView(pmv) {

  private val EditorTypeName: String = Typed.typeToTypeName(classOf[EditorMutableView])
  override def childNodeTypes: Set[String] = super.childNodeTypes + EditorTypeName

  override def childrenNamed(typeName: String): Seq[MutableView[_]] = typeName match {
    case EditorTypeName =>
      pmv.files.asScala.
        filter(f => f.filename.endsWith(RugArchiveProjectType.RugExtension) || f.filename.endsWith(RugArchiveProjectType.TypeScriptExtension)).
        map{fabmv: FileArtifactBackedMutableView => new EditorMutableView(fabmv.currentBackingObject, this)}

    case other =>
      super.childrenNamed(typeName)
  }

}


