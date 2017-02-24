package com.atomist.rug.runtime.plans

import com.atomist.param.{ParameterValue, SimpleParameterValue}
import com.atomist.rug.{InvalidSecretException, MissingSecretException}
import com.atomist.rug.runtime.CommandHandler
import com.atomist.rug.spi.Secret

/**
  * Used by PlanRunner implementations to resolve secrets declared by CommandHandlers
  * and required by RugFunctions
  *
  */
abstract class SecretResolver (handler: CommandHandler) {

  //basically things like `path/to/secret/with?param=this&other=that`
  private val commonSecretRegExpStr = "([\\w]+(/[\\w]+)+)+((\\?[\\w]+\\=[\\w]+)+(\\&[\\w]+\\=[\\w]+)*)*"

  private val secretPathRegExp = s"^$commonSecretRegExpStr$$".r
  private val secretTokenPathRegExp = s"#\\{$commonSecretRegExpStr\\}".r
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
    parameters.map { param =>
      param.getValue match {
        case s: String =>
          val replaced = handler.secrets.foldLeft[String](s) {(cur, secret) =>
            if(secretPathRegExp.findFirstIn(secret.path).isEmpty){
              throw new InvalidSecretException(s"Secret path: ${secret.path} is invalid on Command Handler: ${handler.name}")
            }
            cur.replace(s"#{${secret.path}}", resolveSecret(secret).getValue.toString)
          }
          val unresolved = secretTokenPathRegExp.findAllMatchIn(replaced).toSeq
          if(unresolved.nonEmpty){
            throw new MissingSecretException(s"Found unresolved secrets in parameter: ${param.getName}: ${unresolved.mkString(",")}")
          }
          SimpleParameterValue(param.getName,replaced)
        case _ => param
      }
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
