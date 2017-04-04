package com.atomist.rug.runtime.plans

import com.atomist.param.SimpleParameterValues
import com.atomist.project.archive.RugResolver
import com.atomist.project.edit._
import com.atomist.project.generate.ProjectGenerator
import com.atomist.project.review.ProjectReviewer
import com.atomist.rug.{BadPlanException, BadRugFunctionResponseException}
import com.atomist.rug.runtime._
import com.atomist.rug.runtime.js.RugContext
import com.atomist.rug.spi.Handlers.Instruction._
import com.atomist.rug.spi.Handlers.Status.{Failure, Success}
import com.atomist.rug.spi.Handlers.{Instruction, Response}
import com.atomist.rug.spi.{Body, RugFunctionRegistry}
import com.atomist.util.JsonUtils

/**
  * Run instructions synchronously in this JVM
  *
  * TODO - ensure we blow up if there are rugs or instructions with duplicate names
  * and don't have different GA's
  */
class LocalInstructionRunner(currentRug: Rug,
                             projectManagement: ProjectManagement,
                             rugContext: RugContext,
                             secretResolver: SecretResolver,
                             rugFunctionRegistry: RugFunctionRegistry = DefaultRugFunctionRegistry,
                             rugResolver: Option[RugResolver] = None)
  extends InstructionRunner
  with PlanSupport{

  private def doWithProjectName(instruction: Instruction, action: (String) => Response) = {
    instruction.detail.projectName match {
      case Some(projectName) => action(projectName)
      case _ => throw new BadPlanException(s"Project name required for $instruction.")
    }
  }

  override def run(instruction: Instruction, callbackInput: Option[Response]): Response = {
    val parameters = SimpleParameterValues(instruction.detail.parameters)
    instruction match {
      case Execute(detail) =>
        rugFunctionRegistry.find(detail.name) match {
          case Some(fn) =>
            val replaced = secretResolver.replaceSecretTokens(detail.parameters)
            val resolved = SimpleParameterValues(replaced ++ secretResolver.resolveSecrets(fn.secrets))
            val response = fn.run(resolved)

            //ensure the body is String or byte[]!
            val thedata = response.body match {
              case Some(Body(Some(str), None)) => Some(str)
              case Some(Body(None, Some(bytes))) => Some(bytes)
              case Some(Body(_,_)) => throw new BadRugFunctionResponseException(s"Function `${fn.name}` should return a string body or a byte array, but not both")
              case _ => None
            }
            Response(response.status, response.msg, response.code, thedata)
          case _ => throw new BadPlanException(s"Cannot find Rug Function ${detail.name}")
        }
      case _ =>
        rugResolver match {
          case Some(resolver) =>
            resolver.resolve(currentRug, extractName(instruction.detail)) match {
              case Some(rug: ProjectGenerator) =>
                doWithProjectName(instruction, (projectName: String) => {
                  projectManagement.generate(rug, parameters, projectName)
                  Response(Success)//TODO serialize the response?
                })
              case Some(rug: ProjectEditor) =>
                doWithProjectName(instruction, (projectName: String) => {
                  projectManagement.edit(rug,parameters, projectName) match {
                    case _: SuccessfulModification => Response(Success)
                    case success: NoModificationNeeded => Response(Success,Some(success.comment))
                    case failure: FailedModificationAttempt => Response(Failure, Some(failure.failureExplanation))
                  }
                })
              case Some(rug: ProjectReviewer) =>
                doWithProjectName(instruction, (projectName: String) => {
                  val reviewResult = projectManagement.review(rug,parameters, projectName)
                  Response(Success, None, None, Some(JsonUtils.toJson(reviewResult)))
                })
              case Some(rug: CommandHandler) =>
                val planOption = rug.handle(rugContext, parameters)
                Response(Success, None, None, planOption)
              case Some(rug: ResponseHandler) =>
                callbackInput match {
                  case Some(response) =>
                    val planOption = rug.handle(response, parameters)
                    Response(Success, None, None, planOption)
                  case c =>
                    throw new BadPlanException(s"Callback input was not recognized: $c")
                }
              case Some(rug) => throw new BadPlanException(s"Unrecognized rug: $rug")
              case None => throw new BadPlanException(s"Could not find rug with name: ${instruction.detail.name}")
            }
          case _ => throw new IllegalArgumentException(s"Could not find rug with name: ${instruction.detail.name} because no RugResolver supplied")
        }

    }
  }

  /**
    * Convert Instruction.Detail name/coords to a string for resolver
    * @param detail
    * @return
    */
  def extractName(detail: Instruction.Detail): String = {
    detail.coordinates match {
      case Some(coords) =>
        s"${coords.group}:${coords.artifact}:${detail.name}"
      case _ => detail.name
    }
  }
}
