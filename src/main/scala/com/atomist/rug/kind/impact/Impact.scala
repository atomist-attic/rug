package com.atomist.rug.kind.impact

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.core.{FileMutableView, ProjectMutableView}
import com.atomist.rug.spi.{ExportFunction, TypeProvider}
import com.atomist.source._
import com.atomist.tree.{ParentAwareTreeNode, SimpleTerminalTreeNode, TreeNode}

/**
  * Represents the impact of one or more changes to a project.
  * Allows finding file deltas ("files"), and the changed (new or updated) files ("changed").
  */
class Impact(_parent: GraphNode, _before: ArtifactSource, _after: ArtifactSource)
  extends TreeNode {

  private lazy val deltas: Deltas = _after Δ _before

  private lazy val beforeProject = new ProjectMutableView(_before)

  private lazy val afterProject = new ProjectMutableView(_after)

  @ExportFunction(readOnly = true, description = "Project before impact", exposeAsProperty = true)
  def before: ProjectMutableView = beforeProject

  @ExportFunction(readOnly = true, description = "Project after impact", exposeAsProperty = true)
  def after: ProjectMutableView = afterProject

  @ExportFunction(readOnly = true, description = "Parent of the impact", exposeAsProperty = true)
  def parent: GraphNode = _parent

  override def nodeName: String = "impact"

  override def value: String = ???

  override def childrenNamed(key: String): Seq[TreeNode] = key match {
    case "files" =>
      files
    case "changed" =>
      Seq(changed)
    case _ => Nil
  }

  @ExportFunction(readOnly = true, description = "File impacts in this commit", exposeAsProperty = true)
  def files: Seq[FileImpact] =
    deltas.deltas collect {
      case fad: FileAdditionDelta =>
        new FileAddition(this, new FileMutableView(fad.newFile, afterProject))
      case fud: FileUpdateDelta =>
        new FileUpdate(this, new FileMutableView(fud.oldFile, beforeProject), new FileMutableView(fud.updatedFile, afterProject))
      case fdd: FileDeletionDelta =>
        new FileDeletion(this, new FileMutableView(fdd.oldFile, beforeProject))
    }

  @ExportFunction(readOnly = true, description = "Virtual project composed of files changed in this commit", exposeAsProperty = true)
  def changed: ProjectMutableView = {
    val filesToInclude =
      deltas.deltas collect {
        case fad: FileAdditionDelta =>
          fad.newFile
        case fud: FileUpdateDelta =>
          fud.updatedFile
      }
    new ProjectMutableView(new SimpleFileBasedArtifactSource(_after.id, filesToInclude))
  }

  override val childNodeNames: Set[String] = Set("files", "changed", "before", "after")

  override val childNodeTypes: Set[String] = Set("FileImpact", "FileAddition", "FileUpdate", "FileDeletion")

  override def toString =
    s"Impact: fileImpacts=${files.mkString(",")}"

}

abstract class FileImpact(_parent: TreeNode) extends ParentAwareTreeNode {

  @ExportFunction(readOnly = true, description = "Path to the file", exposeAsProperty = true)
  def path: String

  @ExportFunction(readOnly = true, description = "Parent", exposeAsProperty = true)
  override def parent: TreeNode = _parent

  override def value: String = ???

  override def nodeName: String = getClass.getSimpleName

  override def toString = s"${getClass.getSimpleName}:$path"

}

class FileAdditionTypeProvider extends TypeProvider(classOf[FileAddition]) {

  override def description: String = "File addition"
}

class FileAddition(_parent: Impact, newFile: FileMutableView)
  extends FileImpact(_parent) {

  @ExportFunction(readOnly = true, description = "Path to the file", exposeAsProperty = true)
  override def path: String = newFile.path

  @ExportFunction(readOnly = true, description = "File", exposeAsProperty = true)
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

  @ExportFunction(readOnly = true, description = "Path to the file", exposeAsProperty = true)
  override def path: String = oldFile.path

  @ExportFunction(readOnly = true, description = "File", exposeAsProperty = true)
  def old: FileMutableView = oldFile

  @ExportFunction(readOnly = true, description = "File", exposeAsProperty = true)
  def now: FileMutableView = newFile

  override def childrenNamed(key: String): Seq[TreeNode] = key match {
    case "path" =>
      Seq(SimpleTerminalTreeNode("path", path))
    case "old" =>
      Seq(old)
    case "new" =>
      Seq(now)
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

  @ExportFunction(readOnly = true, description = "Path to the file", exposeAsProperty = true)
  override def path: String = oldFile.path

  @ExportFunction(readOnly = true, description = "File", exposeAsProperty = true)
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