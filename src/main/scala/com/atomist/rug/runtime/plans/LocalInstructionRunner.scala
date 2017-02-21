package com.atomist.rug.runtime.plans

import com.atomist.param.SimpleParameterValues
import com.atomist.project.edit._
import com.atomist.project.generate.ProjectGenerator
import com.atomist.project.review.ProjectReviewer
import com.atomist.rug.runtime._
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
                             commandContext: CommandContext,
                             rugFunctionRegistry: RugFunctionRegistry = DefaultRugFunctionRegistry)
  extends InstructionRunner
  with PlanSupport{

  private def doWithProjectName(instruction: Instruction, action: (String) => Response) = {
    instruction.detail.projectName match {
      case Some(projectName) => action(projectName)
      case _ => Response(Failure, None, None, Some(s"Project name required for $instruction."))
    }
  }

  override def run(instruction: Instruction, callbackInput: AnyRef): Response = {
    val parameters = SimpleParameterValues(instruction.detail.parameters)
    instruction match {
      case Execute(detail) =>
        rugFunctionRegistry.find(detail.name) match {
          case Some(fn) => fn.run(SimpleParameterValues(detail.parameters))
          case _ => Response(Failure,None, None, Some(s"Cannot find RugFunction ${detail.name}"))
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
            val planOption = rug.handle(commandContext, parameters)
            Response(Success, None, None, planOption)
          case Some(rug: ResponseHandler) =>
            callbackInput match {
              case response: InstructionResponse =>
                val planOption = rug.handle(response, parameters)
                Response(Success, None, None, planOption)
              case _ =>
                Response(Failure, None, None, Some("Callback input was not recognized."))
            }
          case rug => Response(Failure, None, None, Some(s"Cannot execute rug $rug."))
        }
    }
  }
}
