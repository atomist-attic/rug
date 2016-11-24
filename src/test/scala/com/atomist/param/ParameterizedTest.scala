package com.atomist.param

import org.scalatest.{FlatSpec, Matchers}

class ParameterizedTest extends FlatSpec with Matchers {

  import ParametersToTest._

  it should "validate valid parameters" in {
    val pvs = SimpleParameterValues fromMap Map(StringParam.getName -> "anything goes", AgeParam.getName -> "34", ParamStartingWithX.getName -> "x5")
    ParameterizedToTest.areValid(pvs) should be(true)
  }

  it should "reject complete but invalid parameters" in {
    val pvs = SimpleParameterValues fromMap Map(StringParam.getName -> "anything goes", AgeParam.getName -> "xy", ParamStartingWithX.getName -> "x5")
    ParameterizedToTest.areValid(pvs) should be(false)
  }

  it should "reject complete but still invalid parameters" in {
    val pvs = SimpleParameterValues fromMap Map(StringParam.getName -> "anything goes", AgeParam.getName -> "128", ParamStartingWithX.getName -> "yz5")
    ParameterizedToTest.areValid(pvs) should be(false)
  }

  it should "reject incomplete but valid parameters" in {
    val pvs = SimpleParameterValues fromMap Map(StringParam.getName -> "anything goes", ParamStartingWithX.getName -> "x5")
    ParameterizedToTest.areValid(pvs) should be(false)
  }
}
