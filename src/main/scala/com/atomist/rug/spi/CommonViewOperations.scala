package com.atomist.rug.spi

import com.atomist.project.review.{ReviewComment, Severity}
import com.atomist.rug.RugRuntimeException
import com.atomist.rug.runtime.rugdsl.FunctionInvocationContext
import com.typesafe.scalalogging.LazyLogging

/**
  * Common operations on views.
  *
  * @tparam  T type of the underlying object
  */
trait CommonViewOperations[T] extends MutableView[T] with LazyLogging {

  import Severity._

  @ExportFunction(readOnly = false,
    description = "Operate on this. Use when you want to operate on an object in an embedded language such as JavaScript or Clojure")
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
    description = "Cause the editor to print to the console. " +
      "Useful for debugging if running editors locally.")
  final def println(@ExportFunctionParameterDescription(name = "msg",
    description = "The message to be displayed")
                    msg: String): Unit = {
    Console.println(msg)
  }

  @ExportFunction(readOnly = true,
    description = "Report a minor problem")
  final def minorProblem(@ExportFunctionParameterDescription(name = "msg",
    description = "The message to be displayed")
                         msg: String, ic: FunctionInvocationContext[_]): Unit = review(msg, POLISH, ic)

  @ExportFunction(readOnly = true,
    description = "Report a major problem")
  final def majorProblem(@ExportFunctionParameterDescription(name = "msg",
    description = "The message to be displayed")
                         msg: String, ic: FunctionInvocationContext[_]): Unit = review(msg, MAJOR, ic)

  @ExportFunction(readOnly = true,
    description = "Report a severe, blocking problem")
  final def blockingProblem(@ExportFunctionParameterDescription(name = "msg",
    description = "The message to be displayed")
                            msg: String, ic: FunctionInvocationContext[_]): Unit = review(msg, BROKEN, ic)

  private def review(msg: String, severity: Severity, ic: FunctionInvocationContext[_]): Unit = {
    logger.debug(s"Adding review comment $msg in $this")
    if (ic.reviewContext == null) {
      throw new RugRuntimeException(null, s"Unable to raise review comment: No ReviewContent. Probably an internal error")
    }
    ic.reviewContext.comment(toReviewComment(msg, severity))
  }

  /**
    * Subclasses can override this to supply more information about comments.
    */
  protected def toReviewComment(msg: String, severity: Severity): ReviewComment =
    ReviewComment(msg, severity)
}
