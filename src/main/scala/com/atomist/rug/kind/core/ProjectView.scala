package com.atomist.rug.kind.core

import com.atomist.rug.spi.{ExportFunction, ExportFunctionParameterDescription}
import com.atomist.source._

/**
  * Readonly operations on projects.
  */
trait ProjectView
  extends ArtifactContainerView[ArtifactSource] {

  @ExportFunction(readOnly = true,
    exposeAsProperty = true,
    description = "Return the name of the project. If it's in GitHub, it will be the repo name. " +
      "If it's on the local filesystem it will be the directory name")
  def name: String

  @ExportFunction(readOnly = true,
    exposeAsProperty = true,
    description = "The total number of files in this project")
  def fileCount: Int

  @ExportFunction(readOnly = false,
    exposeAsProperty = true,
    description = "Files in this archive")
  def files: java.util.List[FileMutableView]

  @ExportFunction(readOnly = true,
    description = "Does a file with the given path exist and have the expected content?")
  def fileHasContent(@ExportFunctionParameterDescription(name = "path",
    description = "The path to use")
                     path: String,
                     @ExportFunctionParameterDescription(name = "content",
                       description = "The content to check against the given file")
                     content: String): Boolean

  @ExportFunction(readOnly = true,
    description = "The number of files directly in this directory")
  def countFilesInDirectory(@ExportFunctionParameterDescription(name = "path",
    description = "The path to use")
                            path: String): Int

  @ExportFunction(readOnly = true,
    description = "Does a file with the given path exist and have the expected content?")
  def fileContains(@ExportFunctionParameterDescription(name = "path",
    description = "The path to use")
                   path: String,
                   @ExportFunctionParameterDescription(name = "content",
                     description = "The content to check")
                   content: String): Boolean

  @ExportFunction(readOnly = true, description = "Return a new Project View based on the original backing object (normally the .atomist/ directory)")
  def backingArchiveProject(): ProjectMutableView

  @ExportFunction(readOnly = true,
    exposeAsProperty = true,
    exposeResultDirectlyToNashorn = true,
    description = "Provides access additional context, such as the PathExpressionEngine")
  def context: ProjectContext

  @ExportFunction(readOnly = true,
    description = "Return the path expression to this point in the given file")
  def pathTo(path: String, kind: String, lineFrom1: Int, colFrom1: Int): String
}
