package com.atomist.rug.spi

import com.atomist.rug.runtime.ParameterizedRug

/**
  * Add secrets to a ParameterizedRug
  */
trait SecretAwareRug extends ParameterizedRug{
  def secrets: Seq[Secret]
}
