package com.atomist.rug.kind.core

import com.atomist.rug.spi.{ExportFunction, ExportFunctionParameterDescription, MutableView, ViewSupport}
import com.atomist.source.{ArtifactContainer, DirectoryArtifact, FileArtifact}

object ArtifactContainerMutableView {

  val FileAlias = "file"

  val DirectoryAlias = "directory"

}

import com.atomist.rug.kind.core.ArtifactContainerMutableView._

abstract class ArtifactContainerMutableView[T <: ArtifactContainer](
                                                                     originalBackingObject: T,
                                                                     parent: MutableView[_])
  extends ViewSupport[T](originalBackingObject, parent) {

  override def childNodeTypes: Set[String] = Set(FileAlias, DirectoryAlias)

  override def childrenNames: Seq[String] =
    currentBackingObject.artifacts.map(_.name)

  override def nodeName: String = name

  def name: String

  protected def kids(fieldName: String, parent: ProjectMutableView): Seq[MutableView[_]] = fieldName match {
    case FileAlias =>
      currentBackingObject.allFiles.view.map(f => new FileArtifactMutableView(f, parent))
    case DirectoryAlias =>
      currentBackingObject.allDirectories.view.map(d => new DirectoryArtifactMutableView(d, parent))
    case maybeContainedArtifactName =>
      val arts = currentBackingObject.artifacts.filter(_.name.equals(maybeContainedArtifactName))
      arts.map {
        case d : DirectoryArtifact => new DirectoryArtifactMutableView(d, parent)
        case f : FileArtifact => new FileArtifactMutableView(f, parent)
      }
  }

  @ExportFunction(readOnly = true, description = "Return the number of files in this project")
  def fileCount: Int = currentBackingObject.totalFileCount

  @ExportFunction(readOnly = true, description = "Does a file with the given path exist?")
  def fileExists(@ExportFunctionParameterDescription(name = "path",
    description = "The path to use")
                 path: String): Boolean = currentBackingObject.findFile(path).isDefined

  @ExportFunction(readOnly = true, description = "Does a directory with the given path exist?")
  def directoryExists(@ExportFunctionParameterDescription(name = "path",
    description = "The path to use")
                      path: String): Boolean = currentBackingObject.findDirectory(path).isDefined
}

class DirectoryArtifactMutableView(
                                    originalBackingObject: DirectoryArtifact,
                                    override val parent: ProjectMutableView)
  extends ArtifactContainerMutableView[DirectoryArtifact](originalBackingObject, parent) {

  override def nodeType: String = DirectoryAlias

  @ExportFunction(readOnly = true,
    description = "Return the name of the directory")
  override def name: String = {
    val n = currentBackingObject.name
    n
  }

  override def children(fieldName: String): Seq[MutableView[_]] =
    kids(fieldName, parent)

}