package com.atomist.util.script

import com.atomist.source.FileArtifact

/**
  * Identifies a script.
  */
case class Script(name: String, content: String)

/**
  * Facade for scripts implemented in JavaScript or some other sandboxed
  * resource. The validate() method allows for validation of
  * script during startup phase.
  */
trait ScriptBacked {

  /**
    * Check if this script is valid.
    *
    * @throws InvalidScriptException if the script is invalid
    */
  @throws[InvalidScriptException]
  def validate(): Unit
}

class InvalidScriptException(msg: String, e: Exception)
  extends Exception(msg, e)
