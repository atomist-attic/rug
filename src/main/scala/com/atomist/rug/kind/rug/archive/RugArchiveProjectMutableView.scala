package com.atomist.rug.kind.rug.archive

import com.atomist.rug.kind.core.{FileArtifactBackedMutableView, ProjectMutableView}
import com.atomist.rug.kind.rug.dsl.{RugMutableView, RugType}
import com.atomist.rug.kind.support.ProjectDecoratingMutableView
import com.atomist.rug.spi.{ExportFunction, MutableView, Typed}
import com.atomist.rug.ts.RugTranspiler

import scala.collection.JavaConverters._

class RugArchiveProjectMutableView(pmv: ProjectMutableView)
  extends ProjectDecoratingMutableView(pmv) {

  @ExportFunction(readOnly = false, description = "Change a .rug to a .ts editor")
  def convertToTypeScript(r: RugMutableView): Unit = {
    println ("I was called! I am your friend!")
    val rugDsl = r.currentBackingObject.content
    val rugPath = r.path
    deleteFile(rugPath)
    val transpiler = new RugTranspiler()
    val ts = transpiler.transpile(rugDsl)
    addFile(transpiler.rugPathToTsPath(rugPath), ts)
    println("I deleted a file and added one, I swear I did something")
  }

  val RugTypeName = Typed.typeToTypeName(classOf[RugMutableView])
  override def childNodeTypes: Set[String] = super.childNodeTypes + RugTypeName

  override def childrenNamed(typeName: String): Seq[MutableView[_]] = typeName match {
    case RugTypeName =>
      pmv.files.asScala.
        filter(_.filename.endsWith(RugType.RugExtension)).
        map{fabmv: FileArtifactBackedMutableView => new RugMutableView(fabmv.currentBackingObject, this)}

    case other =>
      super.childrenNamed(typeName)
  }

}
