package com.atomist.rug.runtime.plans

import com.atomist.param.{ParameterValues, ParameterizedSupport}
import com.atomist.rug.MissingSecretException
import com.atomist.rug.spi.MappedParameterizedRug

/**
  * Add support for validating mapped parameters
  */
trait MappedParameterSupport
  extends ParameterizedSupport
    with MappedParameterizedRug {

  /**
    * Validate normal params first, then secrets!
    *
    * @param poa arguments to validate
    */
  override def validateParameters(poa: ParameterValues): Unit = {
    val defaulted = addDefaultParameterValues(poa)
    super.validateParameters(defaulted)

    val mapped = poa.parameterValueMap

    mappedParameters.foreach { s =>
      if (!mapped.contains(s.localKey)) {
        throw new MissingSecretException(s"$name invocation is missing mapped parameter '${s.foreignKey}' on field '${s.localKey}'")
      }
    }
  }
}
