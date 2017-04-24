package com.atomist.rug.runtime.plans

import com.atomist.param.{ParameterValue, SimpleParameterValue}
import com.atomist.rug.runtime.Rug
import com.atomist.rug.spi.{Secret, SecretAwareRug}

/**
  * Used by PlanRunner implementations to resolve secrets declared by CommandHandlers
  * and required by RugFunctions
  *
  * @param rug - the com mand handler with declared secrets!
  */
abstract class SecretResolver (rug: Option[Rug]) {
  /**
    * Replace things of the form #{secret_path} with secret values in supplied ParameterValues
    *
    * Any replaced secrets _must_ be declared on the handler itself
    *
    * Used to inject secrets in to RugFunction parameters in LocalInstructionRunner.
    * @param parameters
    * @return
    */
  def replaceSecretTokens(parameters: Seq[ParameterValue]): Seq[ParameterValue] = {
    rug match {
      case Some(handler: SecretAwareRug) =>
        parameters.map { param =>
          param.getValue match {
            case s: String =>
              val replaced = handler.secrets.foldLeft[String](s) {(cur, secret) =>
                cur.replace(s"#{${secret.path}}", resolveSecret(secret).getValue.toString)
              }
              SimpleParameterValue(param.getName,replaced)
            case _ => param
          }
        }
      case _ => parameters
    }
  }

  /**
    * Resolve a bunch of secrets at once
    *
    * @param secrets set of Secrets
    * @return a mapping from secret name (could be same as path) to actual secret value
    */
  def resolveSecrets(secrets: Seq[Secret]): Seq[ParameterValue] = {
    secrets.map(s => resolveSecret(s))
  }

  /**
    * Resolve a single secret
    * @param secret
    * @return a mapping from secret name (could be same as path) to actual secret value
    */
  def resolveSecret(secret: Secret) : ParameterValue
}
