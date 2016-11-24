package com.atomist.rug.spi

import scala.collection.Seq

trait TerminalView[T] extends MutableView[T] {

  def childrenNames: Seq[String] = Nil

  override def childNodeTypes: Set[String] = Set()

  def children(fieldName: String): Seq[MutableView[_]] =
    throw new UnsupportedOperationException(s"View of class ${getClass.getSimpleName} has no children. children() should not be called")
}
