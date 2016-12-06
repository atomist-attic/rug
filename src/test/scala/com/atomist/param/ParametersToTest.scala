package com.atomist.param

object ParametersToTest {

  val StringParam = Parameter("name")

  val AgeParam = Parameter("age", "[0-9]+")

  val InputParam = Parameter("input_param", """[a-z][\w]*""")

  val InputParamStrict = Parameter("input_param", """^[a-z][\w]*$""")

  val ParamStartingWithX = Parameter("mystery", "x.*")

  val ParameterizedToTest = new ParameterizedSupport {
    addParameter(StringParam)
    addParameter(AgeParam)
    addParameter(ParamStartingWithX)
  }

  val AllowedValuesParam = Parameter("allowed_value")
    .setMinLength(5)
    .setMaxLength(10)
    .withAllowedValue("foo", "Foo")
    .withAllowedValue("bar", "Bar")
}
