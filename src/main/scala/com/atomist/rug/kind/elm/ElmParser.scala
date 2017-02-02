package com.atomist.rug.kind.elm

import com.atomist.rug.kind.elm.ElmModel.ElmModule
import com.atomist.tree.TreeNode

object ElmParser {

  private def startsWithComment(s: String): Boolean =
    s.trim.startsWith("--") || s.trim.startsWith("{-")

  private def countLeadingSpaces(s: String): Int = {
    s.toIterator.takeWhile(_ == ' ').length
  }

  private def markedLine(currentIndent: Int, previousIndent: Int, line: String): String =
    line match {
      case "" => ""
      case comment if startsWithComment(comment) => comment
      case notIndentedAtAll if currentIndent == 0 => s"${ElmTokens.NewLineAtTheVeryLeft}$notIndentedAtAll"
      case indentedLess if currentIndent < previousIndent =>
        s"${ElmTokens.NewLineWithLessIndentation}$indentedLess"
      case _ => line
    }

  private[elm] def markLinesThatAreLessIndentedX(source: String): String = {
    val markedUp = source.lines.foldLeft((0, Seq[String]())) {
      case ((previousIndent, priorLines), "") =>
        (previousIndent, priorLines :+ "")
      case ((previousIndent, priorLines), line) =>
        val currentIndent = countLeadingSpaces(line)
        val newLine = markedLine(currentIndent, previousIndent, line)
        (currentIndent, priorLines :+ newLine)
    }._2.mkString("", "\n", "\n")

    markedUp
  }

  private[elm] def markLinesThatAreLessIndented(source: String): String = {
    var previousIndent = 0
    var markedUp = ""
    for (line <- source.lines) line match {
      case "" =>
        markedUp += "\n"
      case _ =>
        val currentIndent = countLeadingSpaces(line)
        markedUp += markedLine(currentIndent, previousIndent, line)
        markedUp += "\n"
        previousIndent = currentIndent
    }

    markedUp
  }

  def parse(elmSource: String): ElmModule = {
    val modifiedSource = markLinesThatAreLessIndented(elmSource)
    val em = ElmParserCombinator.parse(modifiedSource)
    em.pad(initialSource = modifiedSource,  isNoise = n => n.significance == TreeNode.Noise, topLevel = true)
    em
  }

  /**
    * Take source we have modified and unmodify it
    *
    * @param modifiedSource
    */
  private[elm] def unmark(modifiedSource: String): String = {
    // TODO we probably want something more sophisticated here, in case $ can appear in Elm identifiers
    modifiedSource.replace("$", "")
  }
}