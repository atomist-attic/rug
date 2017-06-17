package com.atomist.rug.kind.core

import com.atomist.rug.spi.{ExportFunction, MutableView}
import com.atomist.source.DirectoryArtifact

/**
  * Operations on a directory within a project
  * @param parent owning project
  */
class DirectoryMutableView(
                            originalBackingObject: DirectoryArtifact,
                            override val parent: ProjectMutableView)
  extends ArtifactContainerMutableView[DirectoryArtifact](originalBackingObject, parent) {

  @ExportFunction(readOnly = true, description = "Return the name of the directory")
  override def name: String = currentBackingObject.name

  @ExportFunction(readOnly = true, description = "Return the path of the directory")
  def path: String = currentBackingObject.path

  @ExportFunction(readOnly = true, description = "Return the number of files directly in this directory")
  def fileCount: Int = currentBackingObject.files.size

  @ExportFunction(readOnly = true, description = "Node content")
  override def value: String = currentBackingObject.path

  override def childrenNamed(fieldName: String): Seq[MutableView[_]] = kids(fieldName, parent)
}

object DirectoryMutableView {

  def apply(originalBackingObject: DirectoryArtifact, parent: ProjectMutableView) =
    new DirectoryMutableView(originalBackingObject, parent)
}
