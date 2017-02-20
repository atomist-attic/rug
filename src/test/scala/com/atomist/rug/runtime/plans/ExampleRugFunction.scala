package com.atomist.rug.runtime.plans

import com.atomist.param._
import com.atomist.rug.spi.Handlers.{Response, Status}
import com.atomist.rug.spi.RugFunction

/**
  * Returns the value of its only parameter
  */
class ExampleRugFunction
  extends RugFunction
  with ParameterizedSupport{
  super.addParameter(new Parameter("thingy"))

  /**
    * Run the function, return the Response
    *
    * @param parameters
    * @return
    */
  override def run(parameters: ParameterValues): Response = {
    validateParameters(parameters)
    Response(Status.Success,None, None, Some(parameters.parameterValues.head.getValue))
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

