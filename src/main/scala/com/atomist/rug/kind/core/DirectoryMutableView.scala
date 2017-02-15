package com.atomist.rug.kind.core

import com.atomist.rug.spi.{ExportFunction, MutableView}
import com.atomist.source.DirectoryArtifact

class DirectoryMutableView(
                            originalBackingObject: DirectoryArtifact,
                            override val parent: ProjectMutableView)
  extends ArtifactContainerMutableView[DirectoryArtifact](originalBackingObject, parent) {

  @ExportFunction(readOnly = true, description = "Return the name of the directory")
  override def name: String = currentBackingObject.name

  override def childrenNamed(fieldName: String): Seq[MutableView[_]] = kids(fieldName, parent)
}
