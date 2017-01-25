package com.atomist.rug.runtime.rugdsl

import com.atomist.param.ParameterValues
import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.project.review.{ProjectReviewer, ReviewComment, ReviewResult}
import com.atomist.rug.RugReviewer
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.parser.{Computation, RunOtherOperation, ScriptBlockAction, With}
import com.atomist.rug.runtime.lang.ScriptBlockActionExecutor
import com.atomist.rug.spi.TypeRegistry
import com.atomist.source.ArtifactSource

import scala.collection.mutable.ListBuffer

class RugDrivenProjectReviewer(
                                val evaluator: Evaluator,
                                program: RugReviewer,
                                rugAs: ArtifactSource,
                                kindRegistry: TypeRegistry,
                                namespace: Option[String]
                              )
  extends RugDrivenProjectOperation(program, rugAs, kindRegistry, namespace)
    with ProjectReviewer {

  override protected def onSetContext(): Unit = {}

  override def review(as: ArtifactSource, poa: ParameterValues): ReviewResult = {
    val reviewContext = new ReviewContext
    val project = new ProjectMutableView(rugAs, as, atomistConfig = DefaultAtomistConfig)

    program.actions.foreach {
      case wb: With =>
        val idm = buildIdentifierMap(project, poa)
        executedSelectedBlock(
          rugAs, wb, as, reviewContext, project, poa, identifierMap = idm)
      case sba: ScriptBlockAction =>
        val identifierMap = buildIdentifierMap( project, poa)
        scriptBlockActionExecutor.execute(sba, project, ScriptBlockActionExecutor.DEFAULT_SERVICES_ALIAS, identifierMap)
      case run: RunOtherOperation =>
        val rr = runReviewer(run, rugAs, as, poa)
        for (comment <- rr.comments)
          reviewContext.comment(comment)
      case _ => ???
    }
    ReviewResult(s"Reviewer $name: $description", reviewContext.comments)
  }

  override def computations: Seq[Computation] = program.computations

}

class ReviewContext {

  private val _comments = new ListBuffer[ReviewComment]

  def comments: Seq[ReviewComment] = _comments.toList

  def comment(rc: ReviewComment): Unit = {
    _comments.append(rc)
  }
}
