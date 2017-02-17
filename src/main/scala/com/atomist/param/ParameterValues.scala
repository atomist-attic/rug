package com.atomist.param

import scala.language.postfixOps

trait ParameterValues {

  /**
    * Parameters exposed to template. Will include input parameters
    * but there may be additional computed or other parameters.
    */
  def parameterValues: Seq[ParameterValue]

  def parameterValueMap: Map[String, ParameterValue] = parameterValues.map(pv => pv.getName -> pv) toMap

  /**
    * Throws exception if not found.
    */
  @throws[IllegalArgumentException]
  def paramValue(name: String): Any = {
    val matches = parameterValues.filter(p => name.equals(p.getName))
    if (matches.size != 1) throw new IllegalArgumentException(s"Parameter '$name' not found: Had $parameterValues")
    matches.head.getValue
  }

  /**
    * Throws exception if not found or not a string.
    *
    * @param name the param name
    * @return a string
    * @throws IllegalArgumentException if name is not a string
    */
  @throws[IllegalArgumentException]
  def stringParamValue(name: String): String = paramValue(name) match {
    case null => null
    case s: String => s.toString
    case _ => throw new IllegalArgumentException(s"'$name' is not a String")
  }

  override def toString(): String = {
    val parms = new StringBuilder()
    parameterValues.foreach(p => parms.append(s"${p.getName} -> ${p.getValue}\n"))
    parms.toString()
  }
}
