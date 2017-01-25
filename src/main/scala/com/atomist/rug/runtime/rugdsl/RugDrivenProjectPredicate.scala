package com.atomist.rug.runtime.rugdsl

import com.atomist.param.ParameterValues
import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.project.predicate.ProjectPredicate
import com.atomist.project.review.{ReviewComment, Severity}
import com.atomist.rug.RugProjectPredicate
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.parser.{Computation, DoStep, With}
import com.atomist.rug.spi.TypeRegistry
import com.atomist.source.ArtifactSource
import com.atomist.tree.TreeNode

/**
  * A Rug predicate enables use of Rug matching and navigation syntax to return true/false results.
  * A predicate returns true if its with clause matches anything. For example:
  *
  * <code>
  *   predicate Foo
  *   with File f when path = "ThisIsTheDroidYouAreLookingFor"
  * </code>
  */
class RugDrivenProjectPredicate(
                                 val evaluator: Evaluator,
                                 program: RugProjectPredicate,
                                 rugAs: ArtifactSource,
                                 kindRegistry: TypeRegistry,
                                 namespace: Option[String])
  extends RugDrivenProjectOperation(program, rugAs, kindRegistry, namespace)
    with ProjectPredicate {

  override protected def onSetContext(): Unit = {}

  override def holds(as: ArtifactSource, poa: ParameterValues): Boolean = {
    val reviewContext = new ReviewContext
    val project = new ProjectMutableView(rugAs, as, atomistConfig = DefaultAtomistConfig)

    program.actions.foreach {
      case wb: With =>
        val idm = buildIdentifierMap(project, poa)
        executedSelectedBlock(
          rugAs, wb, as, reviewContext, project, poa, identifierMap = idm)
      case _ => ???
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
                                       poa: ParameterValues,
                                       identifierMap: Map[String, Object],
                                       t: TreeNode): PartialFunction[DoStep, Object] = {
    case _ =>
      reviewContext.comment(ReviewComment("anything will do", Severity.FINE))
      null
  }

  override def computations: Seq[Computation] = program.computations
}
