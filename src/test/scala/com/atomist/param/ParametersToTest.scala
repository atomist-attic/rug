package com.atomist.param

object ParametersToTest {

  val StringParam = Parameter("name")

  val AgeParam = Parameter("age", "[0-9]+")

  val InputParam = Parameter("input_param", """^[a-z]\w*$""")

  val InputParamStrict = Parameter("input_param", """^[a-z][\w]*$""")

  val ParamStartingWithX = Parameter("mystery", "x.*")

  val ParameterizedToTest = new ParameterizedSupport {
    addParameter(StringParam)
    addParameter(AgeParam)
    addParameter(ParamStartingWithX)

    override def name: String = ???

    override def description: String = ???

    override def tags: Seq[Tag] = ???
  }

  val AllowedValuesParam = Parameter("allowed_value")
    .withAllowedValue("foo", "Foo")
    .withAllowedValue("bar", "Bar")
    .withAllowedValue("normal_val", "Normal")
}
