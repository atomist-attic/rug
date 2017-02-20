package com.atomist.tree.content.text

trait Positioned {

  def startPosition: InputPosition

  def endPosition: InputPosition

  def length: Int = endPosition - startPosition
}
