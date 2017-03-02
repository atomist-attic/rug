package com.atomist.rug.spi

import com.atomist.project.review.{ReviewComment, Severity}
import com.typesafe.scalalogging.LazyLogging

/**
  * Common operations on views.
  *
  * @tparam  T type of the underlying object
  */
trait CommonViewOperations[T] extends MutableView[T] with LazyLogging {

  import Severity._

  @ExportFunction(readOnly = false,
    description = "Evaluate, i.e., compile and execute, JavaScript code.")
  final def eval(o: Object) {}

  @ExportFunction(readOnly = false,
    description = "Cause the operation to fail with a fatal error")
  final def fail(@ExportFunctionParameterDescription(name = "msg",
    description = "The message to be displayed")
                 msg: String): Unit = {
    throw new InstantEditorFailureException(msg)
  }

  /**
    * Useful for debugging.
    */
  @ExportFunction(readOnly = true,
    description = "Cause the editor to print to the console. Useful for debugging if running editors locally.")
  final def println(@ExportFunctionParameterDescription(name = "msg",
    description = "The message to be displayed")
                    msg: String): Unit = {
    Console.println(msg)
  }

  /**
    * Subclasses can override this to supply more information about comments.
    */
  protected def toReviewComment(msg: String, severity: Severity): ReviewComment =
    ReviewComment(msg, severity)
}
