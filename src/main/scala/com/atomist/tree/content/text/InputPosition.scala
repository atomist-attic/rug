package com.atomist.tree.content.text

/**
  * Represents a position within an input sequence
  */
trait InputPosition {

  /**
    * Offset from beginning of input, from 0.
    */
  def offset: Int

  def -(that: InputPosition): Int = this.offset - that.offset

  def -(n: Int): OffsetInputPosition = OffsetInputPosition(this.offset - n)

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
    */
  def show: String

  /**
    * Take the content from this position to the end, inclusive
    */
  def takeTo(content: CharSequence, end: InputPosition): CharSequence =
    content.subSequence(this.offset, end.offset)
}
