package com.atomist.rug.runtime.plans

import com.atomist.param._
import com.atomist.rug.spi.Handlers.Status
import com.atomist.rug.spi._

/**
  * Returns the value of its only parameter
  */
class ExampleRugFunction
  extends RugFunction
  with SecretSupport {

  override def parameters = Seq(new Parameter("thingy"))

  override def secrets: Seq[Secret] = {
    if (ExampleRugFunction.clearSecrets) {
      Seq()
    } else {
      Seq(Secret("very", "/secret/thingy"))
    }
  }

  /**
    * Run the function, return the Response.
    */
  override def run(parameters: ParameterValues): FunctionResponse = {
    validateParameters(parameters)
    FunctionResponse(Status.Success,Some("It worked! :p"), Some(204), StringBodyOption(parameters.parameterValues.head.getValue.toString))
  }

  /**
    * Custom keys for this template. Must be satisfied in ParameterValues passed in.
    *
    * @return a list of parameters
    */
  override def name: String = "ExampleFunction"
  override def description: String = "Example function"
  override def tags: Seq[Tag] = Seq(Tag("example","example"))
}

object ExampleRugFunction {
  var clearSecrets = false
}
