package com.atomist.rug.test.gherkin

import com.atomist.project.common.InvalidParametersException

/**
  * Standard world for a scenario that lets us add bindings
  * and subclasses attach further state and helper methods.
  */
abstract class ScenarioWorld(val definitions: Definitions) {

  private var bindings: Map[String,Object] = Map()

  private var _aborted = false

  private var ipe: InvalidParametersException = _

  /**
    * Was the scenario run aborted?
    */
  def aborted: Boolean = _aborted

  /**
    * Abort the scenario run, for example, because a given threw an exception.
    */
  def abort(): Unit = {
    _aborted = true
  }

  def put(key: String, value: Object): Unit = {
    bindings = bindings + (key -> value)
  }

  def get(key: String): Object =
    bindings.get(key).orNull

  def clear(key: String): Unit =
    bindings = bindings - key

  /**
    * Invalid parameters exception that aborted execution, or null
    */
  def invalidParameters: InvalidParametersException = ipe

  def logInvalidParameters(ipe: InvalidParametersException): Unit = {
    this.ipe = ipe
  }

}
