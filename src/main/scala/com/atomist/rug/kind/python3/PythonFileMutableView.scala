package com.atomist.rug.kind.python3

import com.atomist.model.content.text.TreeNodeFinders._
import com.atomist.model.content.text.{MutableContainerTreeNode, MutableTreeNode, TreeNode}
import com.atomist.rug.RugRuntimeException
import com.atomist.rug.kind.core.{LazyFileArtifactBackedMutableView, ProjectMutableView}
import com.atomist.rug.kind.python3.PythonType._
import com.atomist.rug.spi.{ExportFunction, ExportFunctionParameterDescription, MutableView, ViewSupport}
import com.atomist.source.FileArtifact

class PythonFileMutableView(
                             originalBackingObject: FileArtifact,
                             parent: ProjectMutableView)
  extends LazyFileArtifactBackedMutableView(originalBackingObject, parent) {

  val originalParsed = pythonParser.parse(originalBackingObject.content)

  private var currentParsed = originalParsed

  override def dirty = true

  override protected def currentContent: String = currentParsed.value

  override def childrenNames: Seq[String] = Seq(ImportAlias)

  override def children(fieldName: String): Seq[MutableView[_]] = fieldName match {
    case ImportAlias =>
      val imports = findByName("import_name", currentParsed)
      imports collect {
        case mv: MutableContainerTreeNode => new ImportMutableView(mv, this)
      }
    case _ => throw new RugRuntimeException(null, s"No child with name '$fieldName' in ${getClass.getSimpleName}")
  }

  @ExportFunction(readOnly = false, description = "Append content")
  def append(
              @ExportFunctionParameterDescription(name = "newContent",
                description = "Content to append to the file")
              newContent: String): Unit = {
    val appended = currentContent + "\n" + newContent
    currentParsed = pythonParser.parse(appended)
  }
}

class ImportMutableView(
                         originalBackingObject: MutableContainerTreeNode,
                         parent: PythonFileMutableView)
  extends ViewSupport[MutableContainerTreeNode](originalBackingObject, parent) {

  override def nodeName: String = "import"

  override def nodeType: String = "import"

  override def childNodeTypes: Set[String] = childNodeNames

  override def childrenNames: Seq[String] = Nil

  override def children(fieldName: String): Seq[MutableView[_]] = Nil

  @ExportFunction(readOnly = true, description = "Import name")
  def name: String = {
    importNode.value
  }

  private def importNode: TreeNode = {
    findByName("dotted_name", currentBackingObject).head
  }

  // TODO this can be pulled into generic functionality also
  @ExportFunction(readOnly = false, description = "Set import name")
  def setName(newName: String): Unit = {
    importNode match {
      case mtn: MutableTreeNode => mtn.update(newName)
    }
  }
}
