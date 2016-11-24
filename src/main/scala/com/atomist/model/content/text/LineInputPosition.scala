package com.atomist.model.content.text

trait LineInputPosition extends InputPosition {

  def lineFrom1: Int

  def colFrom1: Int

  def input: String

  override def show = {
    val lines = input.lines.toSeq
    val gotoLine = Integer.min(lineFrom1 - 1, lines.size - 1)
    val badLine = lines(gotoLine)
    val errorLine = List.fill(colFrom1 - 1)(" ").mkString("") + "^"
    badLine + "\n" + errorLine
  }

  override def toString =
    s"${getClass.getSimpleName}: $lineFrom1/$colFrom1: offset=$offset"
}

/**
  * InputPosition implementation given line position, from 1:1.
  *
  * @param input input string
  * @param lineFrom1 line (from 1)
  * @param colFrom1 column (from 1)
  */
case class LineInputPositionImpl(input: String, lineFrom1: Int, colFrom1: Int)
  extends LineInputPosition {

  override val offset: Int = {
    var offs = 0
    var line = 1
    var col = 1
    for {
      c <- input
      if !(line == lineFrom1 && col == colFrom1)
    } c match {
      case `c` if c == '\n' || c == '\r' =>
        line += 1
        col = 1
        offs += 1
      case _ =>
        col += 1
        offs += 1
    }

    offs
  }
}
