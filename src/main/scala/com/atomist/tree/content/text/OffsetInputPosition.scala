package com.atomist.tree.content.text

case class OffsetInputPosition(offset: Int) extends InputPosition {

  require(offset >= 0, s"Offset must be >= 0, had $offset")

  override def show: String = s"offset=$offset"

  override def toString: String = show
}

object OffsetInputPosition {

  def startOf(s: String): InputPosition = LineHoldingOffsetInputPosition(s, 0)

  def endOf(s: String): InputPosition = LineHoldingOffsetInputPosition(s, s.length)
}