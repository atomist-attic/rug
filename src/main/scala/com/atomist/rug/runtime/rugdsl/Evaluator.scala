package com.atomist.rug.runtime.rugdsl

import com.atomist.param.ParameterValues
import com.atomist.rug.runtime.rugdsl.Evaluator.FunctionTarget
import com.atomist.source.ArtifactSource
import com.atomist.util.scalaparsing.ToEvaluate

object Evaluator {

  type FunctionTarget = Object
}

/**
  * Evaluates functions and literals
  */
trait Evaluator {

  def evaluate[T <: FunctionTarget, R](
                                        te: ToEvaluate,
                                        as: ArtifactSource,
                                        reviewContext: ReviewContext,
                                        target: T,
                                        alias: String,
                                        identifierMap: Map[String, Object],
                                        poa: ParameterValues): R
}
