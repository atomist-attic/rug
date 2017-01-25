package com.atomist.param

case class SimpleParameterValues(parameterValues: Seq[ParameterValue])
  extends ParameterValues

object SimpleParameterValues {

  def apply(pvs: ParameterValue*): ParameterValues =
    SimpleParameterValues(pvs)

  def apply(m: Map[String, Object]): ParameterValues = fromMap(m)

  def apply(k: String, o: Object) : ParameterValues = fromMap(Map(k -> o))

  def fromMap(m: Map[String, Object]): ParameterValues =
    SimpleParameterValues((m map {
      case (k, v) => SimpleParameterValue(k, v)
    }).toSeq)

  val Empty = new SimpleParameterValues(Seq())
}