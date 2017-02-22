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
}
