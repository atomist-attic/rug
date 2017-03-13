package com.atomist.rug.test.gherkin

import com.atomist.param.{ParameterValues, SimpleParameterValues}
import com.atomist.project.archive.Rugs
import com.atomist.project.common.InvalidParametersException
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.runtime.js.interop.{NashornUtils, jsPathExpressionEngine}
import com.atomist.rug.spi.{TypeOperation, TypeRegistry, Typed, UsageSpecificTypeRegistry}
import jdk.nashorn.api.scripting.ScriptObjectMirror

/**
  * Standard world for a scenario that lets us add bindings
  * and subclasses attach further state and helper methods.
  */
abstract class ScenarioWorld(val definitions: Definitions, rugs: Option[Rugs]) {

  private var bindings: Map[String,Object] = Map()

  private var _aborted = false

  private var ipe: InvalidParametersException = _

  private var tr: TypeRegistry = new UsageSpecificTypeRegistry(DefaultTypeRegistry, Seq(
    new Typed() {
      override val name = "Person"
      override def description: String = name
      override def allOperations: Seq[TypeOperation] = Seq(
        //new TypeOperation("name", "", true, Nil, "String", null, None)
      )
      override def operations: Seq[TypeOperation] = allOperations
    },
    new Typed() {
      override val name = "GitHubId"
      override def description: String = name
      override def allOperations: Seq[TypeOperation] = Seq(
        //new TypeOperation("id", "", true, Nil, "String", null, None)
      )
      override def operations: Seq[TypeOperation] = allOperations
    }
  ))

  def typeRegistry: TypeRegistry = tr

  def registerType(t: Typed): Unit = {
    this.tr = new UsageSpecificTypeRegistry(this.tr, Seq(t))
  }

  /**
    * Target for each test. Defaults to the world. First parameter to step functions
    */
  def target: AnyRef = this

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

  protected def parameters(params: Any): ParameterValues = {
    val m: Map[String, Object] = params match {
      case som: ScriptObjectMirror =>
        // The user has created a new JavaScript object, as in { foo: "bar" },
        // to pass up as an argument to the invoked editor. Extract its properties
        NashornUtils.extractProperties(som)
    }
    SimpleParameterValues(m)
  }

}
