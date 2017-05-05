package com.atomist.rug.spi

import com.typesafe.scalalogging.LazyLogging

/**
  * Common operations on views.
  *
  * @tparam  T type of the underlying object
  */
trait CommonViewOperations[T] extends MutableView[T] with LazyLogging {

  @ExportFunction(readOnly = false,
    description = "Cause the operation to fail with a fatal error")
  @Deprecated
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
  @Deprecated
  final def println(@ExportFunctionParameterDescription(name = "msg",
    description = "The message to be displayed")
                    msg: String): Unit = {
    Console.println(msg)
  }

}
