package com.atomist.rug.runtime.plans

import com.atomist.param.SimpleParameterValues
import com.atomist.project.edit._
import com.atomist.project.generate.ProjectGenerator
import com.atomist.project.review.ProjectReviewer
import com.atomist.rug.BadPlanException
import com.atomist.rug.runtime._
import com.atomist.rug.runtime.js.RugContext
import com.atomist.rug.spi.Handlers.Instruction._
import com.atomist.rug.spi.Handlers.Status.{Failure, Success}
import com.atomist.rug.spi.Handlers.{Instruction, Response}
import com.atomist.rug.spi.RugFunctionRegistry

/**
  * Run instructions synchronously in this JVM
  *
  * TODO - ensure we blow up if there are rugs or instructions with duplicate names
  * and don't have different GA's
  */
class LocalInstructionRunner(rugs: Seq[AddressableRug],
                             projectManagement: ProjectManagement,
                             rugContext: RugContext,
                             secretResolver: SecretResolver,
                             rugFunctionRegistry: RugFunctionRegistry = DefaultRugFunctionRegistry)
  extends InstructionRunner
  with PlanSupport{

  private def doWithProjectName(instruction: Instruction, action: (String) => Response) = {
    instruction.detail.projectName match {
      case Some(projectName) => action(projectName)
      case _ => throw new BadPlanException(s"Project name required for $instruction.")
    }
  }

  override def run(instruction: Instruction, callbackInput: AnyRef): Response = {
    val parameters = SimpleParameterValues(instruction.detail.parameters)
    instruction match {
      case Execute(detail) =>
        rugFunctionRegistry.find(detail.name) match {
          case Some(fn) =>
            val replaced = secretResolver.replaceSecretTokens(detail.parameters)
            val resolved = SimpleParameterValues(replaced ++ secretResolver.resolveSecrets(fn.secrets))
            fn.run(resolved)
          case _ => throw new BadPlanException(s"Cannot find Rug Function ${detail.name}")
        }
      case _ =>
        findMatch(rugs, instruction) match {
          case Some(rug: ProjectGenerator) =>
            doWithProjectName(instruction, (projectName: String) => {
              val artifact = projectManagement.generate(rug, parameters, projectName)
              Response(Success, None, None, Some(artifact))
            })
          case Some(rug: ProjectEditor) =>
            doWithProjectName(instruction, (projectName: String) => {
              projectManagement.edit(rug,parameters, projectName) match {
                case success: SuccessfulModification => Response(Success, None, None, Some(success))
                case success: NoModificationNeeded => Response(Success, None, None, Some(success))
                case failure: FailedModificationAttempt => Response(Failure, None, None, Some(failure))
              }
            })
          case Some(rug: ProjectReviewer) =>
            doWithProjectName(instruction, (projectName: String) => {
              val reviewResult = projectManagement.review(rug,parameters, projectName)
              Response(Success, None, None, Some(reviewResult))
            })
          case Some(rug: CommandHandler) =>
            val planOption = rug.handle(rugContext, parameters)
            Response(Success, None, None, planOption)
          case Some(rug: ResponseHandler) =>
            callbackInput match {
              case response: InstructionResponse =>
                val planOption = rug.handle(response, parameters)
                Response(Success, None, None, planOption)
              case c =>
                throw new BadPlanException(s"Callback input was not recognized: $c")
            }
          case Some(rug) => throw new BadPlanException(s"Unrecognized rug: $rug")
          case None => throw new BadPlanException(s"Could not find rug with name: ${instruction.detail.name}")
        }
    }
  }
}
