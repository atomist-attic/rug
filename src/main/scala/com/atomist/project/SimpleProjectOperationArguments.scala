package com.atomist.project

import com.atomist.param.{ParameterValue, SimpleParameterValue}

case class SimpleProjectOperationArguments(
                                            name: String,
                                            parameterValues: Seq[ParameterValue])
  extends ProjectOperationArguments

object SimpleProjectOperationArguments {

  val Empty = new ProjectOperationArguments {
    override val name = ""
    override val parameterValues: Seq[ParameterValue] = Seq[ParameterValue]()
  }

  def apply(name: String, m: Map[String, Object]): ProjectOperationArguments =
    SimpleProjectOperationArguments(name,
      m.toList.map(tup => SimpleParameterValue(tup._1, tup._2).asInstanceOf[ParameterValue]))

  def singleParam(name: String, k: String, v: Object): ProjectOperationArguments =
    apply(name, Map[String, Object](k -> v))
}