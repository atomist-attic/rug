package com.atomist.param

import com.atomist.project.common.{IllformedParametersException, InvalidParametersException, MissingParametersException}

import scala.collection.mutable.ListBuffer

/**
  * Support trait for implementations of Parameterized.
  */
trait ParameterizedSupport extends Parameterized {

  private val params = new ListBuffer[Parameter]

  override final def parameters: Seq[Parameter] = params.filterNot(_ == null)

  protected def addParameter(tp: Parameter): Unit = {
    if(!params.exists(p => tp.name == p.name)){
      params += tp
    }
  }

  protected def addParameters(tps: Seq[Parameter]): Unit = tps.foreach(p => addParameter(p))

  /**
    * Fill out any default values not present in pvs but are required
    * @param pvs parameters so far
    * @return with defaults
    */
  def addDefaultParameterValues(pvs: ParameterValues) : ParameterValues = {
    val toDefault = parameters.filter(p => !pvs.parameterValueMap.contains(p.getName) && p.getDefaultValue != "")
    toDefault match {
      case parms: Seq[Parameter] if parms.nonEmpty => {
        val newParams = parms.map(p => SimpleParameterValue(p.getName,p.getDefaultValue))
        new SimpleParameterValues(newParams ++ pvs.parameterValues)
      }
      case _ => pvs
    }
  }
  /**
    * Validate the given arguments, throwing an exception if they're invalid.
    *
    * @param poa arguments to validate
    * @throws InvalidParametersException if the parameters are invalid
    */
  @throws[InvalidParametersException]
  protected def validateParameters(poa: ParameterValues) {
    val missingParameters = findMissingParameters(poa)
    if (missingParameters.nonEmpty)
      throw new MissingParametersException(s"Missing parameters: [${missingParameters.map(p => p.getName).mkString(",")}]" +
        s": $poa")

    def validEmptyOptionalValue(v: Any) = v == null || "".equals(v)

    // TODO could consider pulling this up to Parameterized
    val validationErrors =
      for {
        pv <- poa.parameterValues
        param <- parameters.find(_.name.equals(pv.getName))
        // Only validate optional values if they're supplied
        if param.isRequired || !validEmptyOptionalValue(pv.getValue)
        if !param.isValidValue(pv.getValue)
      } yield pv

    if (validationErrors.nonEmpty)
      throw new IllformedParametersException(s"Invalid parameters: [[${validationErrors.map(p => p.getName + "=" + p.getValue).mkString(",")}]" +
        s": $poa")
  }
}
