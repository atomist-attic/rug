package com.atomist.rug.runtime.plans

import com.atomist.param.{ParameterValue, SimpleParameterValue}
import com.atomist.rug.MissingSecretException
import com.atomist.rug.runtime.CommandHandler
import com.atomist.rug.spi.Secret

/**
  * Created by kipz on 24/02/2017.
  */
class TestSecretResolver(handler: CommandHandler)
  extends SecretResolver(handler) {
  /**
    * Resolve a single secret
    *
    * @param secret
    * @return a mapping from secret name (could be same as path) to actual secret value
    */
  override def resolveSecret(secret: Secret): ParameterValue = {
    SimpleParameterValue(secret.name,
      secret.path match {
      case "secret/path" => "super-secret-value-1"
      case "atomist/showmethemoney" => "super-secret-value-2"
      case "atomist/user_token" => "super-secret-value-3"
      case "secret/path/blah?key=value&other=blah" => "super-secret-value-4"
      case _ => throw new MissingSecretException(s"Unable to find secret: ${secret.path}")
    })
  }
}
