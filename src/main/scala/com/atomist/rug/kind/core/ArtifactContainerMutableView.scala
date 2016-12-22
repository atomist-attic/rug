package com.atomist.rug.kind.core

import com.atomist.rug.spi._
import com.atomist.source.{ArtifactContainer, DirectoryArtifact, FileArtifact}

object ArtifactContainerMutableView {

  val FileAlias = "file"

  val DirectoryAlias = "directory"

}

abstract class ArtifactContainerMutableView[T <: ArtifactContainer](
                                                                     originalBackingObject: T,
                                                                     parent: MutableView[_])
  extends ViewSupport[T](originalBackingObject, parent) {

  val FileTypeName = Typed.typeToTypeName(classOf[FileMutableView])
  val DirectoryTypeName = Typed.typeToTypeName(classOf[DirectoryMutableView])
  override def childNodeTypes: Set[String] = Set(FileTypeName, DirectoryTypeName)

  override def childrenNames: Seq[String] = currentBackingObject.artifacts.map(_.name)

  override def nodeName: String = name

  def name: String

  protected def kids(fieldName: String, parent: ProjectMutableView): Seq[MutableView[_]] = fieldName match {
    case FileTypeName =>
      currentBackingObject.allFiles.view.map(f => new FileMutableView(f, parent))
    case DirectoryTypeName =>
      currentBackingObject.allDirectories.view.map(d => new DirectoryMutableView(d, parent))
    case maybeContainedArtifactName =>
      val arts = currentBackingObject.artifacts.filter(_.name.equals(maybeContainedArtifactName))
      arts.map {
        case d : DirectoryArtifact => new DirectoryMutableView(d, parent)
        case f : FileArtifact => new FileMutableView(f, parent)
      }
  }

  @ExportFunction(readOnly = true, description = "Return the number of files in this project")
  def fileCount: Int = currentBackingObject.totalFileCount

  @ExportFunction(readOnly = true, description = "Find file with the given path. Return null if not found.")
  def findFile(@ExportFunctionParameterDescription(name = "path",
    description = "Path of the file we want")
                 path: String): FileMutableView = {
    val parent: ProjectMutableView = this match {
      case pmv: ProjectMutableView => pmv
      case dmv: DirectoryMutableView => dmv.parent
    }
    currentBackingObject.findFile(path) match {
      case None => null
      case Some(f) => new FileMutableView(f, parent)
    }
  }

  @ExportFunction(readOnly = true, description = "Does a file with the given path exist?")
  def fileExists(@ExportFunctionParameterDescription(name = "path",
    description = "The path to use")
                 path: String): Boolean = currentBackingObject.findFile(path).isDefined

  @ExportFunction(readOnly = true, description = "Does a directory with the given path exist?")
  def directoryExists(@ExportFunctionParameterDescription(name = "path",
    description = "The path to use")
                      path: String): Boolean = currentBackingObject.findDirectory(path).isDefined
}

class DirectoryMutableView(
                            originalBackingObject: DirectoryArtifact,
                            override val parent: ProjectMutableView)
  extends ArtifactContainerMutableView[DirectoryArtifact](originalBackingObject, parent) {

  @ExportFunction(readOnly = true, description = "Return the name of the directory")
  override def name: String = currentBackingObject.name

  override def children(fieldName: String): Seq[MutableView[_]] = kids(fieldName, parent)
}