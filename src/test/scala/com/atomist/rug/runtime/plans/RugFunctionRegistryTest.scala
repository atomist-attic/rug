package com.atomist.rug.runtime.plans

import com.atomist.param.{SimpleParameterValue, SimpleParameterValues}
import com.atomist.rug.MissingSecretException
import com.atomist.rug.spi.Handlers.Status
import com.atomist.rug.spi._
import org.scalatest.{FlatSpec, Matchers}

class RugFunctionRegistryTest extends FlatSpec with Matchers {


  it should "find and run SuccessRugFunction from the registry" in {
    val fn = DefaultRugFunctionRegistry.find("success").get.asInstanceOf[SuccessRugFunction]
    fn.run(SimpleParameterValues()) match {
      case FunctionResponse(Status.Success, None, None, None) =>
      case _ => ???
    }
  }

  it should "find and run a RugFunction from the registry" in {
    val fn = DefaultRugFunctionRegistry.find("ExampleFunction").get.asInstanceOf[ExampleRugFunction]
    ExampleRugFunction.clearSecrets = true
    fn.run(SimpleParameterValues(SimpleParameterValue("thingy", "woot"))) match {
      case FunctionResponse(Status.Success, _, _, Some(Body(Some(str), None))) => assert(str === "woot")
      case _ => ???
    }
  }

  it should "find and run a RugFunction from the registry via fq classname" in {
    val fn = DefaultRugFunctionRegistry.find("com.atomist.rug.runtime.plans.ExampleRugFunction").get.asInstanceOf[ExampleRugFunction]
  }

  it should "fail if a secret is not set in the parameter list" in {
    val fn = DefaultRugFunctionRegistry.find("ExampleFunction").get.asInstanceOf[ExampleRugFunction]
    ExampleRugFunction.clearSecrets = false
    assertThrows[MissingSecretException] {
      fn.run(SimpleParameterValues(SimpleParameterValue("thingy", "woot")))
    }
  }

  it should "serialize things to json easily" in {
    val bodyStr = JsonBodyOption(FunctionResponse(Status.Success, Some("woot"), Some(200), StringBodyOption("woot"))).get.str.get
    assert(bodyStr === """{"status":{},"msg":"woot","code":200,"body":{"str":"woot"}}""")

    val listStr = JsonBodyOption(Seq(FunctionResponse(Status.Success, Some("woot"), Some(200), StringBodyOption("woot")), FunctionResponse(Status.Success, Some("woot"), Some(200), StringBodyOption("woot")))).get.str.get
    assert(listStr === """[{"status":{},"msg":"woot","code":200,"body":{"str":"woot"}},{"status":{},"msg":"woot","code":200,"body":{"str":"woot"}}]""")
  }
}
