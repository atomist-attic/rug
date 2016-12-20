package com.atomist.rug.kind.core

import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.kind.dynamic.ContextlessViewFinder
import com.atomist.rug.parser.Selected
import com.atomist.rug.runtime.rugdsl.{DefaultEvaluator, Evaluator}
import com.atomist.rug.spi._
import com.atomist.source.ArtifactSource

/**
  * Type representing a line within a file
  * @param evaluator used to evaluate expressions
  */
class LineType(
                evaluator: Evaluator
              )
  extends Type(evaluator)
    with ReflectivelyTypedType
    with ContextlessViewFinder {

  def this() = this(DefaultEvaluator)

  override def resolvesFromNodeTypes: Set[String] = Set("file")

  override def description = "Represents a line within a text file"

  /** Describe the MutableView subclass to allow for reflective function export */
  override def viewManifest: Manifest[_] = manifest[LineMutableView]

  override protected def findAllIn(rugAs: ArtifactSource, selected: Selected, context: MutableView[_],
                                   poa: ProjectOperationArguments,
                                   identifierMap: Map[String, Object]): Option[Seq[MutableView[_]]] = {
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

  override def nodeName: String = "line"

  @ExportFunction(readOnly = false, description = "Update this line's content")
  def update(@ExportFunctionParameterDescription(name = "s2",
    description = "The content to update this line to")
             s2: String): Unit = {
    updateTo(s2)
  }

  @ExportFunction(readOnly = true, description = "Return this line's content")
  def content(): String = currentBackingObject

  @ExportFunction(readOnly = true, description = "Line number")
  def num: Int = linenum

  /**
    * Update the parent after changing this class. Subclasses can override
    * this implementation, which does nothing.
    */
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
          content()
        else l
        i += 1
        newL
      }
    val newContent = newLines.mkString("\n")
    f.setContent(newContent)
    f
  }
}