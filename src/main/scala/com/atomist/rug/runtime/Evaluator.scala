package com.atomist.rug.runtime

import com.atomist.project.ProjectOperationArguments
import com.atomist.rug.runtime.Evaluator.FunctionTarget
import com.atomist.util.scalaparsing.ToEvaluate
import com.atomist.source.ArtifactSource

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
                                        poa: ProjectOperationArguments): R
}
