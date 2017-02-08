package com.atomist.tree.content.text

/**
  * Created by rod on 2/9/17.
  */
case class LineHoldingOffsetInputPosition(input: CharSequence, offset: Int) extends InputPosition {

  require(offset >= 0, s"Offset must be >= 0, had $offset")

  override def show: String = s"${input.toString.take(offset)}  HERE [${input.toString.drop(offset)}] : offset=$offset"

  override def toString: String = show
}
