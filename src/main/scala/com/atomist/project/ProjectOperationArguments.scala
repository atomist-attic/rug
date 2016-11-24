package com.atomist.project

import com.atomist.param.ParameterValues

/**
  * Data passed to project delta operations.
  * Arguments must correspond to parameters declared
  * by the operation.
  * May optional include a contract associated with this operation.
  **/
trait ProjectOperationArguments extends ParameterValues {

  /**
    * Return the name of the project operation. This should uniquely identify it in a context.
    *
    * @return the name
    */
  def name: String
}
