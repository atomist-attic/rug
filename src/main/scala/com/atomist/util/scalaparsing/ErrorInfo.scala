package com.atomist.util.scalaparsing

/**
  * Support for formatting error information in a standard way.
  * Humans index from 1
  *
  * @param message   message describing the failure
  * @param badInput  input that caused the problem
  * @param line      line from 1
  * @param col       column from 1
  * @param filePath  Path to the offending file within the archive
  * @param rootCause exception, if there was one
  */
case class ErrorInfo(
                      message: String,
                      badInput: String,
                      line: Int,
                      col: Int,
                      filePath: String,
                      rootCause: Throwable = null) {

  override def toString: String =
    s"$filePath:$line:$col: $message\n$showLine"

  // TODO could pass in Scala Position and use longString
  private def showLine = {
    if (line < 1 || col < 1)
      s"Cannot show line: Position ($line/$col) is invalid"
    else {
      val lines = badInput.lines.toSeq
      if (lines.isEmpty)
        "<empty input>"
      else {
        // Humans index from 1
        val gotoLine = Integer.min(line - 1, lines.size - 1)
        val badLine = lines(gotoLine)
        val errorLine = List.fill(col - 1)(" ").mkString("") + "^"
        badLine + "\n" + errorLine
      }
    }
  }

}
