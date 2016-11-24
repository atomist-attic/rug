package com.atomist.param

import scala.collection.mutable.ListBuffer

/**
  * Support trait for implementations of Parameterized.
  */
trait ParameterizedSupport extends Parameterized {

  private val params = new ListBuffer[Parameter]

  override final def parameters: Seq[Parameter] = params.filterNot(_ == null)

  protected def addParameter(tp: Parameter): Unit =
    params += tp

  protected def addParameters(tps: Seq[Parameter]): Unit =
    params ++= tps
}
