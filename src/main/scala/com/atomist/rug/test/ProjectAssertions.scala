package com.atomist.rug.test

import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.spi.{ExportFunction, ExportFunctionParameterDescription}
import com.atomist.source.{ArtifactSource, ArtifactSourceUtils, EmptyArtifactSource}

import scala.collection.JavaConversions._

/**
  * Extends project mutable view with assertions specific to testing
  *
  * @param originalBackingObject
  */
class ProjectAssertions(originalBackingObject: ArtifactSource)
  extends ProjectMutableView(EmptyArtifactSource(""), originalBackingObject) {

  @ExportFunction(readOnly = true,
    description = "Does any file whose path contains the literal string contain the given literal content?")
  def anyFileContains(@ExportFunctionParameterDescription(name = "inPath",
    description = "The path to the file to check")
                      inPath: String,
                      @ExportFunctionParameterDescription(name = "content",
                        description = "The content to look for")
                      content: String): Boolean = {
    currentBackingObject.allFiles
      .filter(f => f.path.contains(inPath))
      .exists(f => f.content.contains(content))
  }

  @ExportFunction(readOnly = true,
    description = "Does any file whose path contains the literal string contain the given literal content?")
  def noFileContains(
                     @ExportFunctionParameterDescription(name = "content",
                       description = "The content to look for")
                     content: String): Boolean =
    !currentBackingObject.allFiles
      .exists(f => f.content.contains(content))

  @ExportFunction(readOnly = true, description = "Diagnostic operation. Dump project files")
  def dumpAll: Boolean = {
    println(ArtifactSourceUtils.prettyListFiles(currentBackingObject))
    true
  }

  @ExportFunction(readOnly = true, description = "Diagnostic operation. Dump this file")
  def dump(@ExportFunctionParameterDescription(name = "path",
    description = "The path and filename to the file that is to be dumped out")
           path: String): Boolean = {
    val f = currentBackingObject.findFile(path)
    println(f match {
      case None => s"WARNING: file '$path' not found!"
      case Some(fa) => s"${fa.path}\n{${fa.content}\n-----------"
    })
    true
  }
}
