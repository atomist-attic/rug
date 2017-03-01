package com.atomist.rug.runtime.plans

import com.atomist.param._
import com.atomist.rug.runtime.RugSupport
import com.atomist.rug.spi.Handlers.{Response, Status}
import com.atomist.rug.spi.{RugFunction, Secret}

import scala.collection.mutable.ListBuffer

/**
  * Returns the value of its only parameter
  */
class ExampleRugFunction
  extends RugFunction
  with SecretSupport
    with RugSupport{

  override def parameters = Seq(new Parameter("thingy"))

  private val _secrets = new ListBuffer[Secret]

  def secrets: Seq[Secret] = _secrets

  def addSecret(secret: Secret): Unit = {
    _secrets += secret
  }

  def clearSecrets = _secrets.clear()
  /**
    * Run the function, return the Response
    *
    * @param parameters
    * @return
    */
  override def run(parameters: ParameterValues): Response = {
    validateParameters(parameters)
    Response(Status.Success,Some("It worked! :p"), Some(204), Some(parameters.parameterValues.head.getValue.toString))
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

