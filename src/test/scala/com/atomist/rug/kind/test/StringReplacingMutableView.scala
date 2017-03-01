package com.atomist.rug.kind.test

import com.atomist.rug.kind.core.{LazyFileArtifactBackedMutableView, ProjectMutableView}
import com.atomist.rug.spi.{ExportFunction, ExportFunctionParameterDescription}
import com.atomist.source.FileArtifact

class StringReplacingMutableView(originalBackingObject: FileArtifact, parent: ProjectMutableView)
  extends LazyFileArtifactBackedMutableView(originalBackingObject, parent) {

  private var _content: String = originalBackingObject.content

  override def nodeTags = Set("ReplacerClj")

  override def currentContent: String = _content

  @ExportFunction(readOnly = false, description = "Replace some stuff")
  def replaceIt(@ExportFunctionParameterDescription(name = "stringToReplace",
    description = "The string to replace in other files") stringToReplace: String,
                @ExportFunctionParameterDescription(name = "stringReplacement",
                  description = "The string replacement") stringReplacement: String) {
    _content = _content.replace(stringToReplace, stringReplacement)
    _content = _content + "otherstuff"
    setPath("newroot/" + path)
  }

  @ExportFunction(readOnly = false, description = "Overloaded")
  def overloaded(@ExportFunctionParameterDescription(name = "p1", description = "1st parameter") p1: String,
                 @ExportFunctionParameterDescription(name = "p2", description = "2nd parameer") p2: String) {
    _content = p1 + p2
  }

  @ExportFunction(readOnly = false, description = "Overloaded")
  def overloaded(@ExportFunctionParameterDescription(name = "p1", description = "1st parameter") p1: String) {
    _content = p1
  }

  @ExportFunction(readOnly = false, description = "Replace some stuff")
  def replaceItNoGlobal(@ExportFunctionParameterDescription(name = "stringToReplace",
    description = "The string to replace in other files") stringToReplace: String,
                        @ExportFunctionParameterDescription(name = "stringReplacement",
                          description = "The string replacement") stringReplacement: String) {
    _content = _content + "otherstuff"
    setPath("newroot/" + path)
  }
}
