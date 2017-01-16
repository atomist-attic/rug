package com.atomist.param

object ParametersToTest {

  val StringParam = Parameter("name", "^.*$")

  val AgeParam = Parameter("age", "^\\d+$")

  val InputParam = Parameter("input_param", """^[a-z]\w*$""")

  val ParamStartingWithX = Parameter("mystery", "^x.*$")

  val ParameterizedToTest = new ParameterizedSupport {
    addParameter(StringParam)
    addParameter(AgeParam)
    addParameter(ParamStartingWithX)
  }

  val AllowedValuesParam = Parameter("test_param", "^(?:foo|something|normal_val|bar)$")
    .setMinLength(3)
    .setMaxLength(10)
    .setDefaultValue("something")
}
