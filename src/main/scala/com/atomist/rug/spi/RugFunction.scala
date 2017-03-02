package com.atomist.rug.spi

import com.atomist.param.ParameterValues
import com.atomist.rug.runtime.js.JsonSerializer
import com.atomist.rug.spi.Handlers.{Response, Status}

/**
  * Arbitrary functions to be executed as a result add 'execute' instructions to a Plan
  * Should be thread safe.
  *
  * Secrets should be injected as normal ParameterValues
  */
trait RugFunction extends SecretAwareRug {
  /**
    * Run the function, return the Response
    * @param parameters
    * @return
    */
  def run(parameters: ParameterValues): FunctionResponse
}

case class FunctionResponse(status: Status, msg: Option[String] = None, code: Option[Int] = None, body: Option[Body] = None)

case class Body(str: Option[String] = None, bytes: Option[Array[Byte]] = None)

object StringBodyOption {
  def apply(body: String) : Option[Body] = {
    Some(Body(str = Some(body)))
  }
}

object JsonBodyOption {
  def apply(body: AnyRef) : Option[Body] = {
    StringBodyOption(JsonSerializer.toJson(body))
  }
}

object ByteBodyOption {
  def apply(body: Array[Byte]) : Option[Body] = {
    Some(Body(bytes =  Some(body)))
  }
}
