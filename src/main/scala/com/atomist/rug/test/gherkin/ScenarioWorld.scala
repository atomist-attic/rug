package com.atomist.rug.test.gherkin

/**
  * Standard world for a scenario that lets us add bindings
  * and subclasses attach further state and helper methods.
  */
class ScenarioWorld(val definitions: Definitions) {

  private var bindings: Map[String,Object] = Map()

  def put(key: String, value: Object): Unit = {
    bindings = bindings + (key -> value)
  }

  def get(key: String): Object =
    bindings.get(key).orNull

  def clear(key: String): Unit =
    bindings = bindings - key

}
