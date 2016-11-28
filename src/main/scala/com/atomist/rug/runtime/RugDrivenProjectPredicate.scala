package com.atomist.rug.runtime

import com.atomist.project.ProjectOperationArguments
import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.project.predicate.ProjectPredicate
import com.atomist.project.review.{ReviewComment, Severity}
import com.atomist.rug.RugProjectPredicate
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.kind.dynamic.ViewFinder
import com.atomist.rug.parser.{Computation, DoStep, With}
import com.atomist.rug.spi.{MutableView, TypeRegistry}
import com.atomist.source.ArtifactSource

/**
  * A Rug predicate enables use of Rug matching and navigation syntax to return true/false results.
  * A predicate returns true if its with clause matches anything. For example:
  *
  * <code>
  *   predicate Foo
  *   with file f when path = "ThisIsTheDroidYouAreLookingFor"
  * </code>
  */
class RugDrivenProjectPredicate(
                                 val evaluator: Evaluator,
                                 val viewFinder: ViewFinder,
                                 program: RugProjectPredicate,
                                 rugAs: ArtifactSource,
                                 kindRegistry: TypeRegistry,
                                 namespace: Option[String])
  extends RugDrivenProjectOperation(program, rugAs, kindRegistry, namespace)
    with ProjectPredicate {

  override protected def onSetContext(): Unit = {}

  override def holds(as: ArtifactSource, poa: ProjectOperationArguments): Boolean = {
    val reviewContext = new ReviewContext
    val project = new ProjectMutableView(rugAs, as, atomistConfig = DefaultAtomistConfig)

    program.actions.foreach {
      case wb: With =>
        val idm = buildIdentifierMap(rugAs, project, as, poa)
        executedSelectedBlock(
          rugAs, wb, as, reviewContext, project, poa, identifierMap = idm)
      //      case sba: ScriptBlockAction =>
      //        val identifierMap = buildIdentifierMap(rugAs, project, project.currentBackingObject, poa)
      //        scriptBlockActionExecutor.execute(sba, project, ScriptBlockActionExecutor.DEFAULT_SERVICES_ALIAS, identifierMap)
      //      case run: RunOtherOperation =>
      //        val rr = runReviewer(run, rugAs, as, poa)
      //        for (comment <- rr.comments)
      //          reviewContext.comment(comment)
    }
    reviewContext.comments.nonEmpty
  }

  /**
    * If a do step actually gets executed, mark the ReviewContext appropriately
    */
  override protected def doStepHandler(rugAs: ArtifactSource,
                                       as: ArtifactSource,
                                       reviewContext: ReviewContext,
                                       withBlock: With,
                                       poa: ProjectOperationArguments,
                                       identifierMap: Map[String, Object],
                                       t: MutableView[_]): PartialFunction[DoStep, Object] = {
    case _ =>
      reviewContext.comment(ReviewComment("anything will do", Severity.FINE))
      null
  }

  override def computations: Seq[Computation] = program.computations
}
