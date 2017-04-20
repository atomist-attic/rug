package com.atomist.rug.kind.impact

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.core.{FileMutableView, ProjectMutableView}
import com.atomist.rug.spi.{ExportFunction, TypeProvider}
import com.atomist.source._
import com.atomist.tree.{ParentAwareTreeNode, SimpleTerminalTreeNode, TreeNode}

/**
  * Represents the impact of one or more changes to a project.
  */
class Impact(_parent: GraphNode, before: ArtifactSource, after: ArtifactSource)
  extends TreeNode {

  private lazy val deltas: Deltas = after Δ before

  private lazy val beforeProject = new ProjectMutableView(before)

  private lazy val afterProject = new ProjectMutableView(after)

  @ExportFunction(readOnly = true, description = "Parent of the impact")
  def parent: GraphNode = _parent

  @ExportFunction(readOnly = true, description = "Name of the node")
  override def nodeName: String = "impact"

  @ExportFunction(readOnly = true, description = "Node content")
  override def value: String = ???

  override def childrenNamed(key: String): Seq[TreeNode] = key match {
    case "files" =>
      deltas.deltas collect {
        case fad: FileAdditionDelta =>
          new FileAddition(this, new FileMutableView(fad.newFile, afterProject))
        case fud: FileUpdateDelta =>
          new FileUpdate(this, new FileMutableView(fud.oldFile, beforeProject), new FileMutableView(fud.updatedFile, afterProject))
        case fdd: FileDeletionDelta =>
          new FileDeletion(this, new FileMutableView(fdd.oldFile, beforeProject))
      }
    case _ => Nil
  }

  override val childNodeNames: Set[String] = Set("files")

  override val childNodeTypes: Set[String] = Set("FileImpact", "FileAddition", "FileUpdate", "FileDeletion")

}

abstract class FileImpact(_parent: TreeNode) extends ParentAwareTreeNode {

  @ExportFunction(readOnly = true, description = "Path of the file")
  def path: String

  @ExportFunction(readOnly = true, description = "Parent")
  override def parent: TreeNode = _parent

  @ExportFunction(readOnly = true, description = "Node content")
  override def value: String = ???

  @ExportFunction(readOnly = true, description = "Name of the node")
  override def nodeName: String = getClass.getSimpleName

}

class FileAdditionTypeProvider extends TypeProvider(classOf[FileAddition]) {

  override def description: String = "File addition"
}

class FileAddition(_parent: Impact, newFile: FileMutableView)
  extends FileImpact(_parent) {

  @ExportFunction(readOnly = true, description = "Path of the file")
  override def path: String = newFile.path

  @ExportFunction(readOnly = true, description = "File")
  def file: FileMutableView = newFile

  override def childrenNamed(key: String): Seq[TreeNode] = key match {
    case "file" =>
      Seq(file)
    case "path" =>
      Seq(SimpleTerminalTreeNode("path", path))
    case _ => Nil
  }

  override val childNodeNames: Set[String] = Set("file", "path")

  override def childNodeTypes: Set[String] = Set("File")

}

class FileUpdateTypeProvider extends TypeProvider(classOf[FileUpdate]) {

  override def description: String = "File update"
}

class FileUpdate(_parent: Impact, oldFile: FileMutableView, newFile: FileMutableView)
  extends FileImpact(_parent) {

  @ExportFunction(readOnly = true, description = "Path of the file")
  override def path: String = oldFile.path

  @ExportFunction(readOnly = true, description = "File")
  def old: FileMutableView = oldFile

  @ExportFunction(readOnly = true, description = "File")
  def now: FileMutableView = newFile

  override def childrenNamed(key: String): Seq[TreeNode] = key match {
    case "path" =>
      Seq(SimpleTerminalTreeNode("path", path))
    case "old" =>
      ???
    case "new" =>
      ???
    case _ => Nil
  }

  override val childNodeNames: Set[String] = Set("path", "old", "new")

  override def childNodeTypes: Set[String] = Set("File")

}


class FileDeletionTypeProvider extends TypeProvider(classOf[FileDeletion]) {

  override def description: String = "File deletion"
}

class FileDeletion(_parent: Impact, oldFile: FileMutableView)
  extends FileImpact(_parent) {

  @ExportFunction(readOnly = true, description = "Path of the file")
  override def path: String = oldFile.path

  @ExportFunction(readOnly = true, description = "File")
  def file: FileMutableView = oldFile

  override def childrenNamed(key: String): Seq[TreeNode] = key match {
    case "file" =>
      Seq(file)
    case "path" =>
      Seq(SimpleTerminalTreeNode("path", path))
    case _ => Nil
  }

  override val childNodeNames: Set[String] = Set("file", "path")

  override def childNodeTypes: Set[String] = Set("File")

}