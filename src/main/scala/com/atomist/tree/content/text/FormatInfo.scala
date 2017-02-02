package com.atomist.tree.content.text

import com.atomist.util.lang.CodeGenerationHelper

object FormatInfo {

  /**
    * Given the left input, derive position information.
    */
  def contextInfo(leftInput: String): PointFormatInfo = {

    def findLeadingWhitespace(s: String): String =
      s.takeWhile(c => c.isWhitespace)

    var lineNumberFrom1 = 1
    var currentLine = ""
    var firstIndentedLine: Option[String] = None
    var offs = 0
    while (offs < leftInput.length) {
      leftInput.charAt(offs) match {
        case '\n' =>
          lineNumberFrom1 += 1
          if (firstIndentedLine.isEmpty && findLeadingWhitespace(currentLine).nonEmpty) {
            firstIndentedLine = Some(currentLine)
          }
          currentLine = ""
        case c =>
          currentLine += c
      }
      offs += 1
    }
    val indent: String = firstIndentedLine.map(l => findLeadingWhitespace(l)).getOrElse("")
    val indentDepth = findLeadingWhitespace(currentLine).length /
      { if (indent.isEmpty) 1 else indent.length }

    PointFormatInfo(
      offs,
      lineNumberFrom1,
      currentLine.length + 1,
      indentDepth,
      indent)
  }

}


/**
  * Information about the format of a node within a file
  */
case class FormatInfo(
                     start: PointFormatInfo,
                     end: PointFormatInfo
                     )

/**
  * Information about the format in the file at a particular point
  *
  * @param indentDepth indent depth at the beginning of this line. Number of times
  *                    the indent appears at the beginning of this line
  * @param indent      indent used in the file.
  */
case class PointFormatInfo(
                       offset: Int,
                       lineNumberFrom1: Int,
                       columnNumberFrom1: Int,
                       indentDepth: Int,
                       indent: String
                     )
  extends InputPosition {

  private val codeGenerationHelper = new CodeGenerationHelper(indent)

  /**
    * Add content indented on next line. Tabs will be used as additional indents.
    * @param content content to add
    * @return indented content, including line break
    */
  def indented(content: String): String = {
    val expanded = content.replaceAll("\\t", indent)
    "\n" + codeGenerationHelper.indented(expanded, indentDepth)
  }

  def usesTabs: Boolean = indent.contains("\t")

  override def show: String = toString
}
