package com.atomist.param

import org.scalatest.{FlatSpec, Matchers}

class ParameterTest extends FlatSpec with Matchers {

  import ParametersToTest._

  it should "accept valid strings" in {
    shouldAccept(StringParam, Seq("12345", "234", "dogs", "tony", "malcolm", "donald", ".23452335"))
  }

  it should "accept valid numbers" in {
    shouldAccept(AgeParam, Seq("1", "30", "300"))
  }

  it should "reject invalid numbers" in {
    shouldReject(AgeParam, Seq("-x", "doggie", ".."))
  }

  it should "accept valid custom parameter values" in {
    shouldAccept(ParamStartingWithX, Seq("x1", "x30", "x300"))
  }

  it should "accept valid custom parameter with max and min lengths specified" in {
    val p = new Parameter("p1").setMaxLength(14).setMinLength(2)
    shouldAccept(p, Seq("validParameter", "x30", "x300"))
  }

  it should "accept parameter with allowed values" in {
    shouldAccept(AllowedValuesParam, Seq("foo", "bar", "normal_val"))
  }

  it should "reject invalid custom parameter values" in {
    shouldReject(ParamStartingWithX, Seq("1y", "doggie", "3.2"))
  }

  it should "reject invalid custom parameter with max and min lengths specified" in {
    val p = new Parameter("p2").setMaxLength(5).setMinLength(3)
    shouldReject(p, Seq("invalid", "x2", "x"))
  }

  it should "reject parameter with allowed and other values" in {
    shouldReject(AllowedValuesParam, Seq("ff", "long_parameter"))
  }

  it should "don't reject parameter that matches the pattern" in {
    shouldAccept(InputParam, Seq("FavoriteColour"))
  }

  it should "don't reject parameter that matches a stricter pattern" in {
    shouldAccept(InputParamStrict, Seq("eColour"))
  }

  it should "reject a parameter that doesn't match the pattern" in {
    shouldReject(InputParamStrict, Seq("FavoriteColour"))
  }

  private def shouldAccept(p: Parameter, values: Seq[String]): Unit = {
    values.foreach(v => p.isValidValue(v) should be(true))
  }

  private def shouldReject(p: Parameter, values: Seq[String]): Unit = {
    values.foreach(v => p.isValidValue(v) should be(false))
  }
}
