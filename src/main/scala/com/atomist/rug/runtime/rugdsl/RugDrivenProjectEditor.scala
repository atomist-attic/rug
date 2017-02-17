package com.atomist.rug.runtime.rugdsl

import com.atomist.param.{ParameterValues, SimpleParameterValues}
import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.project.edit.{ProjectEditorSupport, _}
import com.atomist.project.predicate.ProjectPredicate
import com.atomist.project.review.ProjectReviewer
import com.atomist.project.ProjectOperation
import com.atomist.rug._
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.parser.{Computation, RunOtherOperation, ScriptBlockAction, With}
import com.atomist.rug.runtime.lang.ScriptBlockActionExecutor
import com.atomist.rug.spi.{InstantEditorFailureException, TypeRegistry}
import com.atomist.source.ArtifactSource
import com.atomist.util.Timing
import com.typesafe.scalalogging.LazyLogging

class RugDrivenProjectEditor(
                              val evaluator: Evaluator,
                              program: RugEditor,
                              rugAs: ArtifactSource,
                              kindRegistry: TypeRegistry,
                              namespace: Option[String]
                            )
  extends RugDrivenProjectOperation(program, rugAs, kindRegistry, namespace)
    with ProjectEditorSupport
    with LazyLogging {

  override protected def onSetContext(): Unit =
    (program.preconditions ++ program.postcondition).foreach(pre => {
      if (getCondition(pre).parameters.nonEmpty)
        throw new InvalidRugUsesException(name,
          s"Condition '${pre.predicateOrReviewerName}' has parameters. This is not allowed",
          pre.predicateOrReviewerName)
    })

  override def applicability(artifactSource: ArtifactSource): Applicability = {
    program.preconditions
      .map(pre => Applicability(evaluateCondition(pre, artifactSource), ""))
      .find(!_.canApply)
      .getOrElse(Applicability.OK)
  }

  override def meetsPostcondition(as: ArtifactSource): Boolean =
    program.postcondition.exists(cond => evaluateCondition(cond, as))

  import Timing._

  override protected def modifyInternal(as: ArtifactSource, poa: ParameterValues): ModificationAttempt = {
    val tr = time {
      val reviewContext: ReviewContext = null
      val context = new ProjectMutableView(rugAs, as, atomistConfig = DefaultAtomistConfig)

      var currentProjectState: ProjectMutableView = context
      try {
        program.actions.foreach {
          case wb: With =>
            val identifierMap = buildIdentifierMap(currentProjectState, poa)
            currentProjectState = executedSelectedBlock(rugAs, wb, currentProjectState.currentBackingObject,
              reviewContext, currentProjectState, poa, identifierMap).asInstanceOf[ProjectMutableView]
          case sba: ScriptBlockAction =>
            val identifierMap = buildIdentifierMap(currentProjectState, poa)
            scriptBlockActionExecutor.execute(sba, currentProjectState, ScriptBlockActionExecutor.DEFAULT_PROJECT_ALIAS, identifierMap)
          case roo: RunOtherOperation =>
            runEditor(roo, rugAs, currentProjectState.currentBackingObject, poa) match {
              case sm: SuccessfulModification =>
                currentProjectState.updateTo(sm.result)
              case nmn: NoModificationNeeded =>
              // Do nothing
              case fm: FailedModificationAttempt =>
                throw new InstantEditorFailureException(s"Editor ${roo.name} failed: ${fm.failureExplanation}")
            }
          case _ =>
        }

        if (program.postcondition.isDefined) {
          if (!evaluateCondition(program.postcondition.get, currentProjectState.currentBackingObject))
            throw new InstantEditorFailureException(s"Postcondition ${program.postcondition.get.predicateOrReviewerName} failed")
        }

        if (currentProjectState.currentBackingObject == as) {
          NoModificationNeeded(
            "OK")
        } else
          SuccessfulModification(currentProjectState.currentBackingObject,
            currentProjectState.changeLogEntries)
      } catch {
        case f: InstantEditorFailureException =>
          FailedModificationAttempt(f.getMessage)
      }
    }
    logger.debug(s"$name modifyInternal took ${tr._2}ms")
    tr._1
  }

  /**
    * If we use a reviewer, it should not identify any comments.
    * If we use a predicate, it should hold. This means that their logic will be the opposite.
    */
  private def evaluateCondition(pre: Condition, as: ArtifactSource): Boolean = getCondition(pre) match {
    case rev: ProjectReviewer => rev.review(as, SimpleParameterValues.Empty).comments.isEmpty
    case pred: ProjectPredicate => pred.holds(as, SimpleParameterValues.Empty)
  }

  private def isMatch(pre: Condition, op: ProjectOperation): Boolean = {
    op.name.equals(pre.predicateOrReviewerName) || {
      namespace match {
        case None => false
        case Some(ns) => op.name.equals(ns + "." + pre.predicateOrReviewerName)
      }
    }
  }

  private def getCondition(pre: Condition): ProjectOperation = {
    operations.find(op => isMatch(pre, op)) match {
      case None =>
        val knownPredicatesOrReviewers = operations collect {
          case p: RugDrivenProjectPredicate => p
          case r: ProjectReviewer => r
        }
        throw new UndefinedRugUsesException(name,
          s"Cannot run unknown reviewer or predicate: ${pre.predicateOrReviewerName}: Candidates were [${knownPredicatesOrReviewers.map(_.name).mkString(",")}]",
          Seq(pre.predicateOrReviewerName))
      case Some(rev: ProjectReviewer) =>
        rev
      case Some(pe: RugDrivenProjectPredicate) =>
        pe
      case Some(wtf) =>
        throw new RugRuntimeException(name, s"Project operation is not a predicate or ProjectReviewer: '${pre.predicateOrReviewerName}'", null)
    }
  }

  override def computations: Seq[Computation] = program.computations
}
