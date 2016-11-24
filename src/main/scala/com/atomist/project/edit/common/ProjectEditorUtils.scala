package com.atomist.project.edit.common

import com.atomist.source._
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConversions._

/**
  * Convenience methods for working with results.
  */
object ProjectEditorUtils extends LazyLogging {

  /**
    * Replace files with the given paths in this ArtifactSource.
    * Add if not found.
    */
  def replace(as: ArtifactSource, modifiedFiles: FileArtifact*): ArtifactSource = {
    val fe = SimpleFileEditor(f => modifiedFiles.exists(mf => {
      logger.debug(s"Comparing ${mf.path} and ${f.path}")
      mf.path equals f.path
    }), f => f)
    val result = as ✎ fe
    val nonExistingFiles: java.util.List[Artifact] = modifiedFiles.filter(mf => as.findFile(mf.path).isEmpty)
    result + nonExistingFiles
  }

  /**
    * Append the given string to a file
    */
  def appendToFile(as: ArtifactSource, targetFile: FileArtifact, what: String): ArtifactSource = {
    val moddedFile = StringFileArtifact(targetFile.path, targetFile.content + what)
    replace(as, moddedFile)
  }

  /**
    * Returns a new or existing FileArtifact.
    *
    * @param as the ArtifactSource
    * @param path the path
    * @param content the content
    * @return a new file or an existing file with the given content
    */
  def newOrExistingFile(as: ArtifactSource, path: String, content: String = ""): FileArtifact =
    as.findFile(path).getOrElse(StringFileArtifact(path, content))
}
