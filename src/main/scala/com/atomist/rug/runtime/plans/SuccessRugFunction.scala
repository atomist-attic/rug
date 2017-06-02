package com.atomist.rug.runtime.plans

import com.atomist.rug.spi.Handlers.Status
import com.atomist.rug.spi.{AnnotatedRugFunction, FunctionResponse}
import com.atomist.rug.spi.annotation.{RugFunction, Tag}

/**
  *  RugFunction that always returns success.
  */
class SuccessRugFunction
  extends AnnotatedRugFunction {

  @RugFunction(name = "success", description = "Returns success; Useful to invoke onSuccess ResponseHandlers in plans")
  def success(): FunctionResponse = {
    FunctionResponse(Status.Success, None, None, None)
  }

}
