package com.atomist.rug.kind.core

/**
  * Extended by files or sections of files (such as Java classes) to report size information.
  */
trait FileMetrics {

  def lineCount: Int
}

object FileMetrics {

  def lineCount(s: String): Int =
    s.lines.size
}