package com.atomist.tree.content.text

/**
  * Represents a position within an input string
  */
trait InputPosition {

  /**
    * Offset from beginning of input, from 0.
    */
  def offset: Int

  def -(that: InputPosition) = this.offset - that.offset

  def -(n: Int) = OffsetInputPosition(this.offset - n)

  /**
    * Advance by a number of characters
    * @param offs number of characters to advance by
    * @return a new offset
    */
  def +(offs: Int): InputPosition = new InputPosition {

    override def offset: Int = InputPosition.this.offset + offs

    override def show: String = InputPosition.this.show

    override def toString = s"[${InputPosition.this}+$offs]"
  }

  /**
    * Show a readable compiler-style position
    *
    * @return
    */
  def show: String
}

case class OffsetInputPosition(offset: Int) extends InputPosition {

  require(offset >= 0, s"Offset must be >= 0, had $offset")

  override def show: String = s"offset=$offset"

  override def toString = show
}

case class LineHoldingOffsetInputPosition(input: CharSequence, offset: Int) extends InputPosition {

  require(offset >= 0, s"Offset must be >= 0, had $offset")

  override def show: String = s"${input.subSequence(0, offset)}  HERE [${input.subSequence(offset, input.length)}] : offset=$offset"

  override def toString = show
}

object OffsetInputPosition {

  def startOf(s: String) = LineHoldingOffsetInputPosition(s, 0)

  def endOf(s: String) = LineHoldingOffsetInputPosition(s, s.length)
}
