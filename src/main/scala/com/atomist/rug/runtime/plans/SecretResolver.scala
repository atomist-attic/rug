package com.atomist.rug.runtime.plans

import com.atomist.param.ParameterValue
import com.atomist.rug.spi.Secret

/**
  * Used by PlanRunner implementations to resolve secrets declared by CommandHandlers
  * and required by RugFunctions
  */
trait SecretResolver {
  /**
    * Resolve a bunch of secrets at once
    * @param secrets set of Secrets
    * @return a mapping from secret name (could be same as path) to actual secret value
    */
  def resolveSecrets(secrets: Seq[Secret]): Seq[ParameterValue]
}
