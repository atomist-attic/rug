package com.atomist.rug.kind.rug.dsl

import com.atomist.rug.kind.core.{LazyFileArtifactBackedMutableView, ProjectMutableView}
import com.atomist.rug.spi.MutableView
import com.atomist.source.FileArtifact

class RugMutableView(
originalBackingObject: FileArtifact,
parent: ProjectMutableView)
  extends LazyFileArtifactBackedMutableView(originalBackingObject, parent) {

  val r =  originalBackingObject.content
  /**
    * Values that can be passed to children method.
    * Ordering is significant. If there is more than one child name,
    * the first returned will be the default.
    */
  override def childrenNames: Seq[String] = Seq()

  // TODO not very nice that we need to express children in terms of MutableView, not View, but it's OK for now
  override def children(fieldName: String): Seq[MutableView[_]] = Seq()

  override def childNodeTypes: Set[String] = Set()

  /**
    * Return current content for the file.
    *
    * @return current content
    */
  override protected def currentContent: String = r
}
