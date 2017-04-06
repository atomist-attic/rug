package com.atomist.rug.kind.core

import com.atomist.graph.GraphNode
import com.atomist.rug.spi.{ExportFunction, _}

/**
  * Type representing a line within a file
  */
class LineType
  extends Type
    with ReflectivelyTypedType {

  override def description = "Represents a line within a text file"

  /** Describe the MutableView subclass to allow for reflective function export */
  override def runtimeClass: Class[LineMutableView] = classOf[LineMutableView]

  override def findAllIn(context: GraphNode): Option[Seq[LineMutableView]] = {
    context match {
      case fa: FileMutableView =>
        Some(fa.originalBackingObject.content.lines
          .zipWithIndex
          .map(tup => new LineMutableView(tup._1, tup._2, fa))
          .toSeq
        )
      case _ => None
    }
  }

}

class LineMutableView(
                       originalBackingObject: String,
                       linenum: Int,
                       override val parent: FileMutableView)
  extends ViewSupport[String](originalBackingObject, parent)
    with TerminalView[String] {

  override def nodeName: String = s"Line#$linenum"

  override def childNodeNames: Set[String] = Set()

  @ExportFunction(readOnly = true,
    exposeAsProperty = true,
    description = "Containing file")
  def file: FileMutableView = parent

  @ExportFunction(readOnly = false,
    description = "Update this line's content")
  def update(@ExportFunctionParameterDescription(name = "s2",
    description = "The content to update this line to")
             s2: String): Unit = {
    updateTo(s2)
  }

  @ExportFunction(readOnly = true, description = "Return this line's content")
  override def value: String = currentBackingObject

  @ExportFunction(readOnly = true, description = "Return this line's content")
  @Deprecated
  def content: String = value

  @ExportFunction(readOnly = true,
    exposeAsProperty = true,
    description = "Line number from 0")
  def num: Int = linenum

  @ExportFunction(readOnly = true,
    exposeAsProperty = true,
    description = "Line number from 1")
  def numFrom1: Int = linenum + 1

  @ExportFunction(readOnly = true,
    exposeAsProperty = true,
    description = "Line length")
  def length: Int = value.length

  override protected def updateParent(): Unit = {
    applied(parent)
  }

  private def applied(f: FileMutableView) = {
    var i = 0
    val newLines =
      for {
        l <- f.content.lines
      } yield {
        val newL = if (i == num)
          value
        else l
        i += 1
        newL
      }
    val newContent = newLines.mkString("\n")
    f.setContent(newContent)
    f
  }

  override def toString: String =
    s"${parent.path}:$numFrom1;value=[$value];hc=$hashCode"
}
