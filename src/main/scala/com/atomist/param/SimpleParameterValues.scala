package com.atomist.param

case class SimpleParameterValues(parameterValues: Seq[ParameterValue])
  extends ParameterValues

object SimpleParameterValues {

  def apply(pvs: ParameterValue*): ParameterValues =
    SimpleParameterValues(pvs)

  def fromMap(m: Map[String, String]): ParameterValues =
    SimpleParameterValues((m map {
      case (k, v) => SimpleParameterValue(k, v)
    }).toSeq)
}