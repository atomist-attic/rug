package com.atomist.rug.kind.java

import com.atomist.model.project.{ConfigValue, Configuration, SimpleConfigValue, SimpleConfiguration}
import com.atomist.project.Extractor
import com.atomist.source.FileArtifact
import org.apache.commons.lang3.StringUtils

import scala.collection.mutable.ListBuffer
import scala.io.Source

/**
  * Read application.properties file and return configuration
  */
class ExtractApplicationProperties(source: String) extends Extractor[FileArtifact, Configuration] {

  override def apply(f: FileArtifact): Configuration = {
    val isWhiteSpace: String => Boolean = line => StringUtils.isWhitespace(line)
    val isComment: String => Boolean = line => !isWhiteSpace(line) && line.dropWhile(c => c.isWhitespace).startsWith("#")
    val isContent: String => Boolean = line => !(isWhiteSpace(line) || isComment(line))

    trait State
    object InComment extends State
    object InBlanks extends State

    var state: State = InComment
    var comment = ""
    val configValues = new ListBuffer[ConfigValue]()

    // Strip # and whitespace from comments (respecting multiline comments)
    def extractComment(comment: String): String = {

      def toCommentContentLine(l: String) = {
        val r = l.dropWhile(c => c.isWhitespace || '#'.equals(c))
        r
      }

      val r = comment.lines.map(l => toCommentContentLine(l)).mkString("\n")
      r
    }

    // Return None if not a valid property line
    def parseContentLine(line: String): Option[ConfigValue] = {
      val stripped = line.dropWhile(c => c.isWhitespace)
      val idx = stripped.indexOf("=")
      if (idx == -1) {
        None
      }
      else {
        val (key, value) = stripped.splitAt(idx)
        val profile = ""
        Some(SimpleConfigValue(key, value.substring(1), source, profile, description = extractComment(comment)))
      }
    }

    def appendToComment(l: String): Unit = {
      if ("".equals(comment)) comment = l
      else comment = comment + "\n" + l
    }

    val lines = Source.fromString(f.content).getLines()
    for (line <- lines) {
      if (isContent(line)) {
        parseContentLine(line).foreach(cv => configValues.append(cv))
        comment = ""
      }
      else state match {
        case InBlanks if isComment(line) =>
          state = InComment
          appendToComment(line)
        case InComment if isComment(line) || isWhiteSpace(line) =>
          appendToComment(line)
        case InComment =>
          comment = ""
          state = InBlanks
        case _ =>
      }
    }
    new SimpleConfiguration(configValues)
  }
}
