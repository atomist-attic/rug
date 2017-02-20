package com.atomist.rug.spi

/**
  * Allows Rug to dynamically find all registered Behaviour
  */
trait RugFunctionRegistry {
  def find(name: String) : Option[RugFunction]
}
