package com.atomist.rug

import com.atomist.project.ProjectOperation
import com.atomist.rug.kind.dynamic.{DefaultViewFinder, ViewFinder}
import com.atomist.rug.parser._
import com.atomist.rug.runtime._
import com.atomist.rug.spi.{StaticTypeInformation, TypeRegistry}
import com.atomist.source.ArtifactSource

/**
  * Compiles Rug programs into ProjectEditor instances
  */
class DefaultRugCompiler(
                          evaluator: Evaluator,
                          typeRegistry: TypeRegistry,
                          viewFinder: ViewFinder = DefaultViewFinder
                        )
  extends RugCompiler {

  @throws[BadRugException]
  override def compile(
                        rugProgram: RugProgram,
                        artifactSource: ArtifactSource,
                        namespace: Option[String],
                        knownOperations: Seq[ProjectOperation]): RugDrivenProjectOperation = {
    for (w <- rugProgram.withs)
      validateWithTypesAndReferences(rugProgram, w)
    validateReferences(rugProgram, knownOperations)
    rugProgram match {
      case ed: RugEditor => new RugDrivenProjectEditor(evaluator, viewFinder, ed, artifactSource, typeRegistry, namespace)
      case rev: RugReviewer => new RugDrivenProjectReviewer(evaluator, viewFinder, rev, artifactSource, typeRegistry, namespace)
      case rpp: RugProjectPredicate => new RugDrivenProjectPredicate(evaluator, viewFinder, rpp, artifactSource, typeRegistry, namespace)
      case ex: RugExecutor => new RugDrivenExecutor(evaluator, viewFinder, ex, artifactSource, typeRegistry, namespace)
    }
  }

  private def validateWithTypesAndReferences(prog: RugProgram, w: With): Unit = {
    // TODO Tidy? Added to allow variables to be used from 'with' blocks
    prog.computations.find(_.name == w.kind) match {
      case Some(_) =>
      case None =>
        val kind = typeRegistry.findByName(w.kind).getOrElse(
          throw new UndefinedRugTypeException(prog.name, s"Extension type '${w.kind}' is unknown", w.kind)
        )
        kind.typeInformation match {
          case st: StaticTypeInformation =>
            val knownOpNames = st.operations.map(_.name)
            val missingFileFunctions = w.doSteps.collect {
              case dds: FunctionDoStep if !knownOpNames.contains(dds.function) =>
                dds.function
            }
            if (missingFileFunctions.nonEmpty) {
              val msg = s"Unknown function reference(s) on type ${w.kind} with alias ${w.alias}: [${missingFileFunctions.mkString(",")}]"
              throw new UndefinedRugFunctionsException(prog.name, msg, missingFileFunctions.toSet)
            }
          case _ => // Can't validate the operation calls
        }
    }
  }

  def validateReferences(rugProgram: RugProgram, knownOperations: Seq[ProjectOperation]): Unit = {
    val undefineds = undefinedReferences(rugProgram, knownOperations)
    if (undefineds.nonEmpty) {
      val msg = s"Script references undefined identifiers: [${undefineds.mkString(",")}]"
      throw new UndefinedRugIdentifiersException(rugProgram.name, msg, undefineds)
    }
  }

  private def undefinedReferences(rugProgram: RugProgram, knownOperations: Seq[ProjectOperation]): Seq[String] = {
    val paramNames = rugProgram.parameters.map(p => p.getName) ++
      rugProgram.computations.map(comp => comp.name)
    rugProgram.withs
      .flatMap(w => w.doSteps)
      .distinct
      .collect {
        case fi: FunctionInvocation => fi
      }
      .flatMap(s => s.args)
      .distinct
      .collect {
        case ir: IdentifierFunctionArg => ir.name
        case WrappedFunctionArg(ir: IdentifierFunctionArg, _) => ir.name
      }
      .filter(ident => !paramNames.contains(ident))
  }
}
