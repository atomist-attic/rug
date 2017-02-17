package com.atomist.rug.runtime.plans

import com.atomist.param.{SimpleParameterValue, SimpleParameterValues}
import com.atomist.rug.spi.Handlers.Response
import com.atomist.rug.spi.Handlers.Status
import org.scalatest.{FlatSpec, Matchers}

class RunFunctionRegistryTest extends FlatSpec with Matchers{
  it should "Find and run a RugFunction from the registry" in {
    val fn = DefaultRugFunctionRegistry.find("ExampleFunction").get
    fn.run(SimpleParameterValues(SimpleParameterValue("thingy", "woot"))) match {
      case Response(Status.Success, _, _, Some(body)) => assert(body == "woot")
      case _ => ???
    }
  }
}
