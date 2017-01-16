package com.atomist.rug.runtime.rugdsl

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Objects

import com.atomist.param.{Parameter, ParameterValue, SimpleParameterValue}
import com.atomist.project.edit._
import com.atomist.project.review.{ProjectReviewer, ReviewResult}
import com.atomist.project.{ProjectOperation, ProjectOperationArguments, SimpleProjectOperationArguments}
import com.atomist.rug.kind.dynamic._
import com.atomist.rug.parser._
import com.atomist.rug.runtime.NamespaceUtils._
import com.atomist.rug.runtime.lang.{DefaultScriptBlockActionExecutor, ScriptBlockActionExecutor}
import com.atomist.rug.spi.{MutableView, TypeRegistry}
import com.atomist.rug.{BadRugSyntaxException, Import, RugRuntimeException}
import com.atomist.source.ArtifactSource
import com.atomist.tree.TreeNode
import com.atomist.tree.utils.TreeNodeUtils
import com.atomist.util.lang.JavaHelpers
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._

object RugOperationSupport {

  val YmlFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d yyyy")

  // May have been passed in via the infrastructure but couldn't be declared in Rug: Suppress
  // so it doesn't upset binding into JavaScript
  private def isIllegalParameterName(k: String) = !JavaHelpers.isValidJavaIdentifier(k)

  def poaToIdentifierMap(parameters: Seq[Parameter],
                         poa: ProjectOperationArguments): Map[String, Object] = {
    val parametersPassedIn = poa.parameterValueMap collect {
      case (k, pv) if !isIllegalParameterName(k) => (k, pv.getValue)
    }
    // Remember to add default parameters that weren't set
    val defaultParameterValuesNotSet: Map[String, String] = parameters.collect {
      case p if !parametersPassedIn.keys.asJavaCollection.contains(p.getName) && p.hasDefaultValue => (p.getName, p.getDefaultValue)
    }.toMap
    parametersPassedIn ++ defaultParameterValuesNotSet
  }
}

/**
  * Useful functionality shared between runtime and test infrastructure.
  */
trait RugOperationSupport extends LazyLogging {

  def viewFinder: ViewFinder

  val kindRegistry: TypeRegistry

  def evaluator: Evaluator

  def name: String

  def computations: Seq[Computation]

  // Be consistent with types of generator-lib
  def parameters: Seq[Parameter]

  def namespace: Option[String]

  def imports: Seq[Import]

  protected val scriptBlockActionExecutor: ScriptBlockActionExecutor = DefaultScriptBlockActionExecutor

  protected def operations: Seq[ProjectOperation]

  /**
    *
    * @return well known identifiers that should be available to all Rug scripts
    */
  // TODO this should work but including any parameters here seems to trigger a bug in JavaScript parameter population
  private def wellKnownIdentifiers: Map[String, Object] = {
    val d = LocalDate.now()
    Map(
      // "date_ymd" -> YmlFormat.format(d)
    )
  }

  protected def buildIdentifierMap(rugAs: ArtifactSource,
                                   context: Object,
                                   project: ArtifactSource,
                                   poa: ProjectOperationArguments): Map[String, Object] = {
    val idm = RugOperationSupport.poaToIdentifierMap(parameters, poa)
    val compMap = computationsMap(rugAs, poa, project, targetAlias = "x", context, identifiersAlreadyResolved = idm)
    idm ++ compMap ++ wellKnownIdentifiers
  }

  /**
    * Return expanded parameters we can pass to other operations. We must include our computations.
    */
  protected def parametersForOtherOperation(rugAs: ArtifactSource,
                                            roo: RunOtherOperation,
                                            poa: ProjectOperationArguments,
                                            project: ArtifactSource) = {
    val context = null
    val idm = buildIdentifierMap(rugAs, context, project, poa)
    val params: Seq[ParameterValue] = (idm.collect {
      case (k, v) => SimpleParameterValue(k, Objects.toString(v))
    } ++ roo.args.collect {
      case arg if arg.parameterName.isDefined && !idm.contains(arg.parameterName.get) =>
        val evaluated = evaluator.evaluate[Object, Any](arg, null, null, null, "roo", idm, poa)
        SimpleParameterValue(arg.parameterName.get, Objects.toString(evaluated))
      case arg if arg.parameterName.isEmpty =>
        throw new RugRuntimeException(name, s"Argument '$arg' is invalid when invoking ${roo.name} as it is unnamed", null)
    }).toSeq
    SimpleProjectOperationArguments(poa.name, params)
  }

  /**
    * Return the computed values (assignments) for this operation
    */
  protected def computationsMap(rugAs: ArtifactSource,
                                poa: ProjectOperationArguments,
                                project: ArtifactSource,
                                targetAlias: String = "p",
                                context: Object,
                                reviewContext: ReviewContext = null,
                                identifiersAlreadyResolved: Map[String, Object]): Map[String, Object] = {
    // We need to keep adding to this as we go
    var knownIdentifiers = identifiersAlreadyResolved
    computations.map {
      case Computation(name, te) =>
        val value = evaluator.evaluate[Object, Object](te, project, reviewContext, context, targetAlias, knownIdentifiers, poa)
        knownIdentifiers += (name -> value)
        (name, value)
    }.toMap
  }

  protected def executedSelectedBlock(rugAs: ArtifactSource,
                                      selected: Selected,
                                      as: ArtifactSource,
                                      reviewContext: ReviewContext,
                                      context: MutableView[_],
                                      poa: ProjectOperationArguments,
                                      identifierMap: Map[String, Object]): Object = {
    val views = findViews(rugAs, selected, context, poa, identifierMap)

    selected match {
      case w: With =>
        for (v <- views) {
          w.doSteps.foreach(step => {
            val idm = identifierMap ++ Map(selected.alias -> v)
            runDoStep(rugAs, as, reviewContext, w, step, poa, idm, v)
          }
          )
          v.commit()
        }
        context
    }
  }

  private def findViews(rugAs: ArtifactSource,
                        selected: Selected,
                        context: MutableView[_],
                        poa: ProjectOperationArguments,
                        identifierMap: Map[String, Object]): Seq[MutableView[_]] = {
    val vo = viewFinder.findIn(rugAs, selected, context, poa, identifierMap)
    vo.getOrElse {
      context.currentBackingObject match {
        case fmv: TreeNode =>
          println(TreeNodeUtils.toShortString(fmv))
        case _ =>
      }
      throw new RugRuntimeException(null, s"Cannot find type '${selected.kind}' under $context using $viewFinder")
    }
  }

  private def runDoStep(rugAs: ArtifactSource,
                        as: ArtifactSource,
                        reviewContext: ReviewContext,
                        withBlock: With,
                        step: DoStep,
                        poa: ProjectOperationArguments,
                        identifierMap: Map[String, Object],
                        t: MutableView[_]): Object = {
    doStepHandler(rugAs, as, reviewContext, withBlock, poa, identifierMap, t)
      .apply(step)
  }

  /**
    * Subclasses can override this to return their own partial function to handle do steps.
    */
  protected def doStepHandler(rugAs: ArtifactSource,
                              as: ArtifactSource,
                              reviewContext: ReviewContext,
                              withBlock: With,
                              poa: ProjectOperationArguments,
                              identifierMap: Map[String, Object],
                              t: MutableView[_]): PartialFunction[DoStep, Object] =
  withOrFunctionDoStepHandler(rugAs, as, reviewContext, withBlock, poa, identifierMap, t)

  /**
    * Well known do step handler subclasses will probably use via orElse in a custom doStepHandler.
    */
  protected def withOrFunctionDoStepHandler(rugAs: ArtifactSource,
                                            as: ArtifactSource,
                                            reviewContext: ReviewContext,
                                            withBlock: With,
                                            poa: ProjectOperationArguments,
                                            identifierMap: Map[String, Object],
                                            context: MutableView[_]): PartialFunction[DoStep, Object] = {
    case fi: FunctionInvocation =>
      context.evaluator.evaluate(fi, as, reviewContext, context, withBlock.alias, identifierMap, poa)
    case w: WithDoStep =>
      executedSelectedBlock(rugAs, w.wth, as, reviewContext, context, poa, identifierMap)
  }

  /**
    * Run the given editor.
    */
  protected def runEditor(roo: RunOtherOperation,
                          rugAs: ArtifactSource,
                          as: ArtifactSource,
                          poa: ProjectOperationArguments): ModificationAttempt = {
    resolve(roo.name, namespace, operations, imports) match {
      case None =>
        throw new RugRuntimeException(name, s"Cannot run unknown operation: ${roo.name}", null)
      case Some(pe: ProjectEditor) =>
        try {
          pe.modify(as, parametersForOtherOperation(rugAs, roo, poa, as))
        } catch expressFailure(roo)
      case Some(wtf) =>
        throw new RugRuntimeException(name, s"Project operation is not a ProjectEditor: ${roo.name}", null)
    }
  }

  private def expressFailure(roo: RunOtherOperation): PartialFunction[Throwable, FailedModificationAttempt] = {
    case a: RugRuntimeException if a.getCause.isInstanceOf[BadRugSyntaxException] =>
      val bre = a.getCause.asInstanceOf[BadRugSyntaxException]
      val detailedDescription =
         s"""While trying to parse:
            |${bre.info.badInput}
            |
            |I encountered this error:
            |${a.getMessage}
           """.stripMargin
      FailedModificationAttempt(detailedDescription, Some(a))
    case nie : NotImplementedError =>
      FailedModificationAttempt(s"NotImplementedError: ??? encountered while running ${roo.name}", Some(nie))
    case other =>
      other.printStackTrace()
      FailedModificationAttempt(s"Failure running ${roo.name}: ${other.getMessage}", Some(other))
  }

  protected def runReviewer(roo: RunOtherOperation,
                            rugAs: ArtifactSource, project: ArtifactSource,
                            poa: ProjectOperationArguments): ReviewResult = {
    resolve(roo.name, namespace, operations, imports) match {
      case None =>
        throw new RugRuntimeException(name, s"Cannot run unknown reviewer: ${roo.name}", null)
      case Some(pr: ProjectReviewer) =>
        pr.review(project, parametersForOtherOperation(rugAs, roo, poa, project))
      case Some(wtf) =>
        throw new RugRuntimeException(name, s"Project operation is not a ProjectEditor: ${roo.name}", null)
    }
  }
}
