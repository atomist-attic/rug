package com.atomist.rug.spi

import scala.collection.Seq

trait TerminalView[T] extends MutableView[T] {

  override def childNodeTypes: Set[String] = Set()

  override def childrenNamed(fieldName: String): Seq[MutableView[_]] =
    throw new UnsupportedOperationException(s"View of class ${getClass.getSimpleName} has no children. children() should not be called")
}
