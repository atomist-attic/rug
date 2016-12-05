package com.atomist.param

/**
  * Extended by anything that takes parameters, declaring what it takes.
  */
trait Parameterized {

  /**
    * Custom keys for this template. Must be satisfied in ParameterValues passed in.
    *
    * @return a list of parameters
    */
  def parameters: Seq[Parameter]

  /**
    * Convenience method subclasses can use to identify any missing parameters.
    *
    * @param pvs argument passed to an operation on this class
    * @return list of any missing parameters
    */
  def findMissingParameters(pvs: ParameterValues): Seq[Parameter] =
    parameters.filter(p => p.isRequired && !pvs.parameterValueMap.exists(_._1 == p.name))

  def findInvalidParameterValues(pvs: ParameterValues): Seq[ParameterValue] = {
    pvs.parameterValues.foldLeft(Nil: Seq[ParameterValue]) { (acc, pv) =>
      parameters.find(_.getName == pv.getName) match {
        case Some(param) if !param.isValidValue(pv.getValue) => acc :+ pv
        case _ => acc
      }
    }
  }

  /**
    * Convenient method to check whether parameter values are valid.
    * Callers should use findMissingParameters and findInvalidParameterValues to
    * find more information if this method returns false.
    *
    * @param pvs ParameterValues to check
    * @return true if the ParameterValues are valid, otherwise false
    */
  def areValid(pvs: ParameterValues): Boolean =
    findMissingParameters(pvs).isEmpty && findInvalidParameterValues(pvs).isEmpty
}
