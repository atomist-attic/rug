package com.atomist.rug.runtime.plans

import com.atomist.param.{SimpleParameterValue, SimpleParameterValues}
import com.atomist.rug.MissingSecretException
import com.atomist.rug.spi.Handlers.{Response, Status}
import com.atomist.rug.spi.Secret
import org.scalatest.{FlatSpec, Matchers}

class RugFunctionRegistryTest extends FlatSpec with Matchers{
  it should "Find and run a RugFunction from the registry" in {
    val fn = DefaultRugFunctionRegistry.find("ExampleFunction").get.asInstanceOf[ExampleRugFunction]
    fn.clearSecrets
    fn.run(SimpleParameterValues(SimpleParameterValue("thingy", "woot"))) match {
      case Response(Status.Success, _, _, Some(body)) => assert(body === "woot")
      case _ => ???
    }
  }

  it should "Find and run a RugFunction from the registry via fq classname" in {
    val fn = DefaultRugFunctionRegistry.find("com.atomist.rug.runtime.plans.ExampleRugFunction").get.asInstanceOf[ExampleRugFunction]

  }

  it should "fail if a secret is not set in the parameter list" in {
    val fn = DefaultRugFunctionRegistry.find("ExampleFunction").get.asInstanceOf[ExampleRugFunction]
    fn.clearSecrets
    fn.addSecret(Secret("very", "/secret/crazy"))
    assertThrows[MissingSecretException]{
      fn.run(SimpleParameterValues(SimpleParameterValue("thingy", "woot")))
    }
  }
}
