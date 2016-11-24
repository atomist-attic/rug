package com.atomist.project.common.script

/**
  * Names of function entry points we'll call in scripts
  */
object ScriptFunctions {

  /**
    * Such a function can return a single file or many
    */
  val GenerateFunction = "generate"

  val ComputedParametersFunction = "computed_parameters"
}
