package com.atomist.rug.runtime.lang

import com.atomist.rug.parser.ScriptBlockAction
import com.atomist.rug.spi.MutableView

/**
  * Implement an action script block.
  */
trait ScriptBlockActionExecutor {

  /**
    * Execute the given script block
    * @param sba script block
    * @param target target of execution
    * @param alias alias for the target in generated code
    * @param identifierMap map of other identifiers to pass in
    * @return result of executing the script block
    */
  def execute(sba: ScriptBlockAction,
              target: Object,
              alias: String,
              identifierMap: Map[String, Object]): Object
}

/**
  * Default aliases to use in arguments to ScriptBlockActionExecutor
  * for well-known types.
  */
object ScriptBlockActionExecutor {

  val DEFAULT_PROJECT_ALIAS = "project"

  val DEFAULT_SERVICES_ALIAS = "services"
}
