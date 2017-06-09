package com.atomist.rug.runtime.plans

import com.atomist.rug.spi.Handlers.Response

/**
  * Convert a Response in some way
  */
trait ResponseConverter {
  def convert(response: Response): Response
}

/**
  * Does nothing to the response
  */
object NullResponseConverter extends ResponseConverter{
  override def convert(response: Response): Response = response
}
