package com.atomist.rug.runtime.plans

import com.atomist.param.{SimpleParameterValue, SimpleParameterValues}
import com.atomist.rug.spi.Handlers.Status
import org.scalatest.{FlatSpec, Matchers}

class AnnotatedRugFunctionTest extends FlatSpec with Matchers{

  it should "report the right field values in an annotation function" in {
    val fn = new ExampleAnnotatedRugFunction()
    assert(fn.name === ExampleAnnotatedRugFunction.theName)
    assert(fn.description === ExampleAnnotatedRugFunction.theDescription)
    assert(fn.tags.size === 1)
    assert(fn.tags.head.name === ExampleAnnotatedRugFunction.theTag)
    assert(fn.secrets.size === 1)
    assert(fn.secrets.head.name === "user_token")
    assert(fn.secrets.head.path === ExampleAnnotatedRugFunction.theSecretPath)
    assert(fn.parameters.size ===4)
    assert(fn.parameters.head.name === "number")

    val res = fn.run(SimpleParameterValues(SimpleParameterValue("number", "100"), SimpleParameterValue("user_token", "woot")))
    assert(res.status === Status.Success)

    val failed = fn.run(SimpleParameterValues(SimpleParameterValue("number", 100.asInstanceOf[AnyRef]), SimpleParameterValue("user_token", "foo")))
    assert(failed.status === Status.Failure)
  }

  it should "optional string parameters with defaults should not end up as null" in {
    val fn = new ExampleAnnotatedRugFunction()
    val res = fn.run(SimpleParameterValues(SimpleParameterValue("number", "100"), SimpleParameterValue("user_token", "woot")))
    assert(res.status === Status.Success)
    assert(fn.blah == "")
  }

  it should "optional int parameters with no default gets converted to 0" in {
    val fn = new ExampleAnnotatedRugFunction()
    val res = fn.run(SimpleParameterValues(SimpleParameterValue("number", "100"), SimpleParameterValue("user_token", "woot")))
    assert(res.status === Status.Success)
    assert(fn.other == 0)
  }

  it should "optional int parameters with default gets converted properly" in {
    val fn = new ExampleAnnotatedRugFunction()
    val res = fn.run(SimpleParameterValues(SimpleParameterValue("number", "100"), SimpleParameterValue("user_token", "woot")))
    assert(res.status === Status.Success)
    assert(fn.yum == 42)
  }
}
