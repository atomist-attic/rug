package com.atomist.param

import com.mifmif.common.regex.Generex

/**
  * Generates values for parameters. This is possible for String
  * parameters using regular expressions or for parameters with default values.
  */
object ParameterGenerator {

  /**
    * Provide a valid parameter value for this parameter. There's no guarantee
    * of randomness: successive calls may or may not return the same value.
    */
  def validValueFor(p: Parameter): ParameterValue = validValueFor(p, 1)

  def validValueFor(p: Parameter, minLength: Int): ParameterValue = p.hasDefaultValue match {
    case true => SimpleParameterValue(p.getName, p.getDefaultValue)
    case false =>
      val generex = new Generex(p.getPattern)
      val generated = generex.random(minLength)
      SimpleParameterValue(p.getName, generated)
  }

  /**
    * Generate valid ParameterValues for this parameter.
    */
  @throws[IllegalArgumentException]
  def validParameterValuesFor(p: Parameterized): ParameterValues =
    new SimpleParameterValues(p.parameters.map(validValueFor))
}
