package com.atomist.rug.runtime.rugdsl

import com.atomist.project.edit.{FailedModificationAttempt, NoModificationNeeded, SuccessfulModification}
import com.atomist.project.{Executor, ProjectOperationArguments, SimpleProjectOperationArguments}
import com.atomist.rug.kind.dynamic.ViewFinder
import com.atomist.rug.kind.service.{ServiceMutableView, ServiceSource, ServicesMutableView}
import com.atomist.rug.parser._
import com.atomist.rug.spi.{MutableView, TypeRegistry}
import com.atomist.rug.{RugExecutor, RugRuntimeException}
import com.atomist.source.ArtifactSource
import com.typesafe.scalalogging.LazyLogging

class RugDrivenExecutor(
                         val evaluator: Evaluator,
                         val viewFinder: ViewFinder,
                         program: RugExecutor,
                         rugAs: ArtifactSource,
                         kindRegistry: TypeRegistry,
                         namespace: Option[String])
  extends RugDrivenProjectOperation(program, rugAs, kindRegistry, namespace)
    with Executor
    with LazyLogging {

  override protected def onSetContext(): Unit = {}

  override def execute(services: ServiceSource, poa: ProjectOperationArguments = SimpleProjectOperationArguments.Empty): Unit = {
    val context = new ServicesMutableView(rugAs, services)

    val reviewContext = new ReviewContext
    val identifierMap = buildIdentifierMap(rugAs, context, null, poa)

    program.actions match {
      case Seq(sba: ScriptBlockAction) =>
        scriptBlockActionExecutor.execute(sba, context, "services", identifierMap)
      case actions =>
        actions.foreach {
          case wb: With =>
            val idm = buildIdentifierMap(rugAs, context, null, poa)
            executedSelectedBlock(
              rugAs, wb, null, reviewContext, context, poa, identifierMap = idm)
        }
    }
  }

  override protected def doStepHandler(rugAs: ArtifactSource,
                                       as: ArtifactSource,
                                       reviewContext: ReviewContext,
                                       withBlock: With,
                                       poa: ProjectOperationArguments,
                                       identifierMap: Map[String, Object],
                                       t: MutableView[_]): PartialFunction[DoStep, Object] = {

    // Apply the given do step corresponding to the result we wanted
    def applyDoStep(doStep: Option[DoStep]): Unit = doStep match {
      case Some(d) => withOrFunctionDoStepHandler(rugAs, as, reviewContext, withBlock, poa, identifierMap, t).apply(d)
      case _ =>
    }

    withOrFunctionDoStepHandler(rugAs, as, reviewContext, withBlock, poa, identifierMap, t).orElse {
      case roo: RunOtherOperation with EditorFlag =>
        // We must be in an Executor, executing against a project
        t match {
          case smv: ServiceMutableView =>
            logger.debug(s"Executor: Updating service with id ${smv.service.project.id}")
            val r = runEditor(roo, rugAs, smv.currentBackingObject, poa)
            r match {
              case sm: SuccessfulModification =>
                smv.updateTo(sm.result, roo)
                applyDoStep(roo.success)
              case fm: FailedModificationAttempt =>
                applyDoStep(roo.failure)
              case nmn: NoModificationNeeded =>
                logger.debug(s"Editor ${roo.name} did not modify anything")
                applyDoStep(roo.noChange)
            }
            smv
          case x => throw new RugRuntimeException(name, s"Cannot run an operation on $x: This can only be done from an executor against a service type")
        }
      case roo: RunOtherOperation with ReviewerFlag =>
        // We must be in an Executor, executing against a project
        t match {
          case smv: ServiceMutableView =>
            val rr = runReviewer(roo, rugAs, smv.currentBackingObject, poa)
            smv.service.reviewOutputPolicy.route(smv.service, rr)
            smv
          case x => throw new RugRuntimeException(name, s"Cannot run an operation on $x: This can only be done from an executor against a service type")
        }
    }
  }

  override def computations: Seq[Computation] = program.computations
}
