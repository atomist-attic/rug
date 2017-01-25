package com.atomist.rug.runtime.rugdsl

import com.atomist.param.{ParameterValues, SimpleParameterValues}
import com.atomist.rug.parser.FunctionInvocation
import com.atomist.source.ArtifactSource

/**
  * Passed to evaluations of function invocations.

  * @tparam T
  */
trait FunctionInvocationContext[T] {

  /** Name of target variable */
  def targetAlias: String

  def functionInvocation: FunctionInvocation

  def args: ParameterValues

  /**
    * Includes identifiers in Rug script plus project parameters.
    * Users should generally use this rather than args.
 *
    * @return all known identifiers
    */
  def identifierMap: Map[String, Object]

  def target: T

  /**
    * Evaluated local arguments to this function invocation, if any.
 *
    * @return local arguments to function invocation
    */
  def localArgs: Seq[Object]

  /**
    * Callees can add to this.
    */
  def reviewContext: ReviewContext

}

trait RugFunction[T, R] {

  // TODO package?
  def name: String

  def description: Option[String]

  def invoke(ic: FunctionInvocationContext[T]): R

  /**
    * Subclasses can override this default (false) to increase efficiency if a function is known to be readonly.
    */
  def readOnly: Boolean = false

}

trait RugPredicate[T]
  extends RugFunction[T, Boolean]

case class SimpleFunctionInvocationContext[T <: Object](
                                                         targetAlias: String,
                                                         functionInvocation: FunctionInvocation,
                                                         target: T,
                                                         deprecated: ArtifactSource,
                                                         reviewContext: ReviewContext,
                                                         identifierMap: Map[String, Object],
                                                         args: ParameterValues = SimpleParameterValues.Empty,
                                                         localArgs: Seq[Object] = Nil)
  extends FunctionInvocationContext[T]
