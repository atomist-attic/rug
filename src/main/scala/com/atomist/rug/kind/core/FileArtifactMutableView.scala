package com.atomist.rug.kind.core

import com.atomist.rug.spi.{ExportFunction, ExportFunctionParameterDescription, TerminalView}
import com.atomist.source.{FileArtifact, StringFileArtifact}

import scala.collection.JavaConversions._

/**
  * The mutable view we expose directly for working with files.
  */
class FileArtifactMutableView(
                               originalBackingObject: FileArtifact,
                               override val parent: ProjectMutableView)
  extends FileArtifactBackedMutableView(originalBackingObject, parent)
    with TerminalView[FileArtifact] {

  @ExportFunction(readOnly = true, description = "Name of the file, excluding path")
  def name = filename

  @ExportFunction(readOnly = true, description = "Is this a Java file?")
  def isJava: Boolean = currentBackingObject.name.endsWith(".java")

  @ExportFunction(readOnly = false, description = "Set entire file content to new string")
  def setContent(@ExportFunctionParameterDescription(name = "newContent",
    description = "The content to set the file to")
                 newContent: String): Unit = {
    updateTo(StringFileArtifact.updated(currentBackingObject, newContent))
  }

  @ExportFunction(readOnly = true,
    description = "Does the file name (not path) contain the given string?")
  def nameContains(@ExportFunctionParameterDescription(name = "what",
    description = "The string to use when looking for it in the file name or path")
                   what: String): Boolean =
    currentBackingObject.name.contains(what)

  @ExportFunction(readOnly = true, description = "Does the file content contain the given string?")
  def contains(@ExportFunctionParameterDescription(name = "what",
    description = "The string to use when looking for it in the file")
               what: String): Boolean =
    currentBackingObject.content.contains(what)

  @ExportFunction(readOnly = true, description = "Does the file content contain a match for the given regex")
  def containsMatch(@ExportFunctionParameterDescription(name = "regexp",
    description = "The regular expression to look for in the file's content")
                    regexp: String): Boolean = {
    val pat = regexp.r
    pat.findFirstIn(currentBackingObject.content).isDefined
  }

  @ExportFunction(readOnly = true,
    description = "Return the first match for the given regex, or the empty string if not found. " +
      "Call containsMatch first to check presence.")
  def firstMatch(@ExportFunctionParameterDescription(name = "regexp",
    description = "The regular expression to search for")
                 regexp: String): String = {
    val pat = regexp.r
    pat.findFirstIn(currentBackingObject.content).getOrElse("")
  }

  @ExportFunction(readOnly = true,
    description = "Return all matches for the given regexp in this file")
  def findMatches(@ExportFunctionParameterDescription(name = "regexp",
    description = "The regular expression to search for")
                  regexp: String): java.util.List[String] = {
    val pat = regexp.r
    pat.findAllIn(currentBackingObject.content).toSeq
  }

  @ExportFunction(readOnly = false,
    description = "Replace all occurrences of the given regexp in this file")
  def regexpReplace(@ExportFunctionParameterDescription(name = "regexp",
    description = "The regular expression to search for")
                    regexp: String,
                    @ExportFunctionParameterDescription(name = "replaceWith",
                      description = "The string to replace matching expressions with")
                    replaceWith: String): Unit = {
    val newContent = currentBackingObject.content.replaceAll(regexp, replaceWith)
    logger.debug(s"Replacing regexp [$regexp] with [$replaceWith] produced [$newContent]")
    updateTo(StringFileArtifact.updated(currentBackingObject, newContent))
  }

  @ExportFunction(readOnly = false,
    description = "Replace all instances of the given literal string in this file")
  def replace(@ExportFunctionParameterDescription(name = "literal",
    description = "The string to search for")
              literal: String,
              @ExportFunctionParameterDescription(name = "replaceWith",
                description = "The string to replace the matches with")
              replaceWith: String): Unit = {
    val newContent = currentBackingObject.content.replace(literal, replaceWith)
    logger.debug(s"Replacing literal [$literal] with [$replaceWith] produced [$newContent]")
    updateTo(StringFileArtifact.updated(currentBackingObject, newContent))
  }

  @ExportFunction(readOnly = false, description = "Append the given content to the file")
  def append(@ExportFunctionParameterDescription(name = "literal",
    description = "The string to append")
             literal: String): Unit = {
    val newContent = currentBackingObject.content + literal
    logger.debug(s"Appended literal [$literal] produced [$newContent]")
    updateTo(StringFileArtifact.updated(currentBackingObject, newContent))
  }

  @ExportFunction(readOnly = false, description = "Prepend the given content to the file")
  def prepend(@ExportFunctionParameterDescription(name = "literal",
    description = "The string to prepend to the file") literal: String): Unit = {
    val newContent = literal + currentBackingObject.content
    logger.debug(s"Appended literal [$literal] produced [$newContent]")
    updateTo(StringFileArtifact.updated(currentBackingObject, newContent))
  }

  @ExportFunction(readOnly = false,
    description = "Change the path to the given value. Path should use forward slashes to denote directories")
  def setPath(@ExportFunctionParameterDescription(name = "newPath",
    description = "The path to change the file to")
              newPath: String): Unit = {
    logger.debug(s"Changed path on $originalBackingObject to $currentBackingObject")
    updateTo(currentBackingObject.withPath(newPath))
  }

  @ExportFunction(readOnly = false, description = "Set the file name, not path, to the given value")
  override def setName(@ExportFunctionParameterDescription(name = "name",
    description = "The name to set the file to") name: String): Unit = super.setName(name)
}
