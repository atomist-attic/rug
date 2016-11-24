package com.atomist.project.common.support

import com.atomist.param.{ParameterValue, ParameterValues, Parameterized}

// TODO pull up to contract-lib as this is generic
trait ParameterComputer {

  /**
    * Compute additional parameters based on passed-in parameters.
    */
  def computedParameters(op: Parameterized, parameterValues: ParameterValues): Seq[ParameterValue]
}
