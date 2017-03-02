package com.atomist.rug.runtime.plans

import com.atomist.rug.spi.Handlers.Response

/**
  * Coerces a Response in some way
  */
trait ResponseCoercer {
  def coerce(response: Response): Response
}


/**
  * Does nothing to the response
  */
object NullResponseCoercer extends ResponseCoercer{
  override def coerce(response: Response): Response = response
}
