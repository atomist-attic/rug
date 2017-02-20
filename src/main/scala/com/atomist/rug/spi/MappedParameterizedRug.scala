package com.atomist.rug.spi

import com.atomist.param.MappedParameter
import com.atomist.rug.runtime.ParameterizedRug

/**
  * Add Mapped parameters
  */
trait MappedParameterizedRug
  extends ParameterizedRug{

  def mappedParameters: Seq[MappedParameter]
}
