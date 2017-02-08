package com.atomist.rug.kind.core

import com.atomist.project.review.ReviewComment
import com.atomist.project.review.Severity.Severity
import com.atomist.rug.spi._
import com.atomist.source.{FileArtifact, StringFileArtifact}

/**
  * Convenience class for MutableView implementations that are backed by a
  * single FileArtifact: For example, an Elm module or a Java source file.
  * Exposes read-only view of file information, such as path, because changing
  * it without considering domain-specific considerations would probably be erroneous.
  */
abstract class FileArtifactBackedMutableView(originalBackingObject: FileArtifact,
                                             override val parent: ProjectMutableView)
 extends ViewSupport[FileArtifact](originalBackingObject: FileArtifact, parent)
    with FileMetrics {

  override def nodeName: String = currentBackingObject.name

  override def childNodeNames: Set[String] = Set()

  override def address = MutableView.address(this, s"path=${currentBackingObject.path}")

  override protected def toReviewComment(msg: String, severity: Severity): ReviewComment =
    ReviewComment(msg, severity, Some(currentBackingObject.path))

  @ExportFunction(readOnly = true, description = "Is this file well-formed?")
  final def isWellFormed: Boolean = wellFormed

  /**
    * Subclasses should override this method to return whether or not their content is
    * well-formed
    * @return
    */
  protected def wellFormed: Boolean = true

  @ExportFunction(readOnly = true, description = "Return file content")
  def content: String = currentBackingObject.content

  @ExportFunction(readOnly = true, description = "Return file name, excluding path")
  def filename: String = currentBackingObject.name

  @ExportFunction(readOnly = true, description = "Return file path, with forward slashes")
  def path: String = currentBackingObject.path

  @ExportFunction(readOnly = true, description = "Return the number of lines in the file")
  override def lineCount: Int = FileMetrics.lineCount(currentBackingObject.content)

  @ExportFunction(readOnly = true, description = "Return the file's permissions")
  def permissions: Int = currentBackingObject.mode

  @ExportFunction(readOnly = false, description = "Make the file executable")
  def makeExecutable(): Unit = updateTo(currentBackingObject.withMode(FileArtifact.ExecutableMode))

  @ExportFunction(readOnly = true,
    description = "Does this path begin with the given pattern? Pattern should contain slashes but not begin with a /")
  def underPath(@ExportFunctionParameterDescription(name = "root",
    description = "The root path to begin searching from") root: String): Boolean =
    path.startsWith(root)

  /**
    * Not to be directly exposed to views in most cases
    *
    * @param newPath new path
    */
  def setPath(newPath: String): Unit

  protected def setName(name: String): Unit = {
    if (!currentBackingObject.path.contains("/")) setPath(name)
    else setPath(currentBackingObject.path.replace("""/""" + currentBackingObject.name, "/" + name))
  }

  override protected def updateParent(): Unit = {
    parent.updateFile(previousBackingObject, currentBackingObject)
  }
}

/**
  * Convenient superclass for views that know how to create new content on demand.
  * They should call the protected setPath method if they need to change the path
  *
  * @param originalBackingObject original FileArtifact
  */
abstract class LazyFileArtifactBackedMutableView(
                                                  originalBackingObject: FileArtifact,
                                                  parent: ProjectMutableView
                                                )
  extends FileArtifactBackedMutableView(originalBackingObject, parent)
    with TerminalView[FileArtifact] {

  private var currentFile = originalBackingObject

  override def dirty: Boolean = currentBackingObject != originalBackingObject

  final override def currentBackingObject: FileArtifact = {
    val newContent = currentContent
    val written = StringFileArtifact.updated(currentFile, newContent)
    if (!written.equals(currentFile)) {
      currentFile = written
      logger.debug(s"Updated file ${currentFile.path}")
    }
    currentFile
  }

  override def setPath(newPath: String): Unit = {
    currentFile = currentBackingObject.withPath(newPath)
  }

  /**
    * Return current content for the file.
    *
    * @return current content
    */
  protected def currentContent: String

  override def commit(): Unit =
    if (dirty)
      parent.updateFile(originalBackingObject, currentBackingObject)
}
