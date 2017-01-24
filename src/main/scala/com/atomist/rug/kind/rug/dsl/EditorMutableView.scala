package com.atomist.rug.kind.rug.dsl

import com.atomist.rug.kind.core.{LazyFileArtifactBackedMutableView, ProjectMutableView}
import com.atomist.rug.kind.rug.archive.RugArchiveProjectType
import com.atomist.rug.spi.ExportFunction
import com.atomist.rug.ts.RugTranspiler
import com.atomist.source.FileArtifact
import com.atomist.tree.TerminalTreeNode

class EditorMutableView(originalBackingObject: FileArtifact,
                        parent: ProjectMutableView)
  extends LazyFileArtifactBackedMutableView(originalBackingObject, parent)
    with TerminalTreeNode {

  @ExportFunction(readOnly = false, description = "Change a .rug to a .ts editor")
  def convertToTypeScript(): Unit = {
    if (isRugDsl) {
      //println("I was called! I am your friend!")
      val rugDsl = currentBackingObject.content
      val rugPath = path
      val transpiler = new RugTranspiler()
      setPath(transpiler.rugPathToTsPath(rugPath))
      val ts = transpiler.transpile(rugDsl)
      _currentContent = ts
      //println("I changed my path and content, I swear I did something")
      commit()
    }
  }

  private var _currentContent = originalBackingObject.content

  private def isRugDsl: Boolean = currentBackingObject.name.endsWith(RugArchiveProjectType.RugExtension)

  private def removeSuffix(suffix: String, whole: String) =
    if (whole.endsWith(suffix))
      whole.substring(0, whole.length - suffix.length)
    else
      whole

  private def removeFileSuffix(filename:String): String =
    {
      removeSuffix(RugArchiveProjectType.TypeScriptExtension,
        removeSuffix(RugArchiveProjectType.RugExtension,
          filename))
    }

//  private def parsedRugDsl = if (isRugFile) {
//    val these = new ParserCombinatorRugParser().parse(originalBackingObject.content)
//    if (these.isEmpty) {
//      throw new RuntimeException(s"Could not parse file ${originalBackingObject.path} as rug")
//    }
//    Some(these)
//  }

  @ExportFunction(readOnly = true, description = "Editor name")
  def name: String = removeFileSuffix(currentBackingObject.name)

  /**
    * Return current content for the file.
    *
    * @return current content
    */
  override protected def currentContent: String = _currentContent
}
