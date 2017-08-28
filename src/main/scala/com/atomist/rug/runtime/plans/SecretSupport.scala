package com.atomist.rug.runtime.plans

import com.atomist.param.{ParameterValues, ParameterizedSupport}
import com.atomist.rug.MissingSecretException
import com.atomist.rug.spi.SecretAwareRug

/**
  * Handle secret/parameter validation
  */
trait SecretSupport
  extends SecretAwareRug
    with ParameterizedSupport {

  /**
    * Validate normal params first, then secrets!
    *
    * @param poa arguments to validate
    */
  override def validateParameters(poa: ParameterValues): Unit = {
    val defaulted = addDefaultParameterValues(poa)
    super.validateParameters(defaulted)

    val mapped = poa.parameterValueMap

    secrets.foreach { s =>
      if (!mapped.contains(s.name)) {
        throw new MissingSecretException(s"$name invocation is missing secret parameter '${s.name}' referring to '${s.path}'")
      }
    }
  }
}
