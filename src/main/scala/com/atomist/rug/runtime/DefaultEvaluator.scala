package com.atomist.rug.runtime

import com.atomist.model.content.grammar.microgrammar.Microgrammar
import com.atomist.model.content.text.{PathExpressionEngine, TreeNode}
import com.atomist.project.ProjectOperationArguments
import com.atomist.rug._
import com.atomist.rug.parser._
import com.atomist.rug.runtime.Evaluator.FunctionTarget
import com.atomist.rug.runtime.lang.js.NashornExpressionEngine
import com.atomist.rug.spi.InstantEditorFailureException
import com.atomist.scalaparsing._
import com.atomist.source.ArtifactSource
import com.typesafe.scalalogging.LazyLogging

object DefaultEvaluator extends DefaultEvaluator(new EmptyRugFunctionRegistry) {

}

class DefaultEvaluator(
                        val functionRegistry: RugFunctionRegistry
                      )
  extends Evaluator
    with LazyLogging {

  private def evaluateArgs[T <: Object](
                                         functionInvocation: FunctionInvocation,
                                         as: ArtifactSource,
                                         rc: ReviewContext,
                                         target: T,
                                         alias: String,
                                         identifierMap: Map[String, Object],
                                         poa: ProjectOperationArguments): Seq[Object] = {
    def evaluateArg: FunctionArg => Object = {
      case fa: FunctionArg => evaluate(fa, as, rc, target, alias, identifierMap, poa)
      case ident: IdentifierFunctionArg =>
        val resolved = identifierMap.get(ident.name)
        resolved.getOrElse(
          throw new UndefinedRugIdentifiersException("unknown", s"Cannot resolve identifier '${ident.parameterName}' referenced in $functionInvocation", Seq(ident.name))
        )
    }

    val evaluatedArgs: Seq[Object] = functionInvocation.args.map(evaluateArg)
    evaluatedArgs
  }

  override def evaluate[T <: FunctionTarget, R](
                                                 te: ToEvaluate,
                                                 as: ArtifactSource,
                                                 reviewContext: ReviewContext,
                                                 target: T,
                                                 alias: String,
                                                 identifierMap: Map[String, Object],
                                                 poa: ProjectOperationArguments): R = {
    try {
      te match {
        case literal: Literal[R] =>
          literal.value
        case _ =>
          val localArgs = te match {
            case fi: FunctionInvocation =>
              evaluateArgs[T](fi, as, reviewContext, target, alias, identifierMap, poa)
            case _ => Nil
          }
          val fi = te match {
            case prf: FunctionInvocation => prf
            case _ => null
          }
          val ic = new SimpleFunctionInvocationContext[T](alias, fi, target, as, reviewContext, identifierMap, poa, localArgs)
          invoke(te, ic).asInstanceOf[R]
      }
    } catch {
      case cce: ClassCastException =>
        throw new RugRuntimeException(null, s"Unexpected return type when evaluating $alias on $target: ${cce.getMessage}", cce)
    }
  }

  protected def invoke[T, R](te: ToEvaluate, ic: FunctionInvocationContext[T]): R = {
    try {
      val rawResult = te match {
        case TruePredicate => true
        case l: SimpleLiteral[_] => l.value
        case WrappedFunctionArg(te, _) => invoke(te, ic)
        case ifa: IdentifierFunctionArg => ic.identifierMap.getOrElse(ifa.name,
          throw new RugRuntimeException(null, s"Undefined identifier '${ifa.name}'", null))
        case ff: FunctionInvocation if ff.args.isEmpty && ic.identifierMap.contains(ff.function) =>
          // The parser can't tell an identifier from a function call here. If there's an identifier,
          // it obscures the function, so use it
          ic.identifierMap(ff.function)
        case ff: FunctionInvocation =>
          val backingFunction = functionRegistry.findByName[T, R](ff.function)
            .getOrElse(
              throw new UndefinedRugFunctionsException(null,
                s"Unregistered function '${ff.function}' on ${ic.target.getClass}: " +
                  s"Known functions [${functionRegistry.functions.mkString(",")}]; target was ${ic.target}",
                Set(ff.function)))
          logger.debug(s"Invoking ${ff.function}")
          val r = backingFunction.invoke(ic)
          r
        case jf: ParsedJavaScriptFunction =>
          val nse = NashornExpressionEngine.evaluator[T](ic, jf.js.content)
          val result = nse.evaluate(ic).asInstanceOf[R]
          result
        case js: JavaScriptBlock =>
          val nse = NashornExpressionEngine.evaluator[T](ic, js.content)
          val result = nse.evaluate(ic).asInstanceOf[R]
          result
        case g4: GrammarBlock =>
          //val g: Microgrammar = InMemAntlrMicrogrammar.lastProduction(g4.content)
          ???
        case PathExpressionValue(pathExpression, None) =>
          val result = pathExpression
          result.asInstanceOf[R]
        case PathExpressionValue(pathExpression, Some(scalarProperty)) =>
          // TODO should cache this
          val pex = new PathExpressionEngine()
          pex.evaluateParsed(ic.target.asInstanceOf[TreeNode], pathExpression) match {
            case Left(err) => throw new InstantEditorFailureException(s"Failed to evaluate [$pathExpression]: $err")
            case Right(nodes) =>
              if (nodes.size != 1)
                throw new InstantEditorFailureException(s"Looking for scalar property [$scalarProperty] in [$pathExpression] but found ${nodes.size} nodes")
              val m = nodes.head.getClass.getMethod(scalarProperty)
              // TODO make this consistent elsewhere. Consider security. And also allow for bean properties.
              // Should we only allow public methods on the type?
              if (m == null) throw new InstantEditorFailureException(s"Looking for scalar property [$scalarProperty] in [$pathExpression] but no method found properties")
              m.invoke(nodes.head)
          }
      }
      rawResult.asInstanceOf[R]
    } catch {
      case t: Throwable =>
        throw t
    }
  }

}
