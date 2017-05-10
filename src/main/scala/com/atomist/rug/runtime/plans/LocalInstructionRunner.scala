package com.atomist.rug.runtime.plans

import com.atomist.param.SimpleParameterValues
import com.atomist.project.archive.RugResolver
import com.atomist.project.edit._
import com.atomist.project.generate.ProjectGenerator
import com.atomist.rug.runtime._
import com.atomist.rug.runtime.js.RugContext
import com.atomist.rug.spi.Handlers.Instruction._
import com.atomist.rug.spi.Handlers.Status.{Failure, Success}
import com.atomist.rug.spi.Handlers.{Instruction, Response, Status}
import com.atomist.rug.spi.{Body, RugFunctionRegistry}
import com.atomist.rug.{BadPlanException, BadRugFunctionResponseException}
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Try, Failure => ScalaFailure, Success => ScalaSuccess}

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
                             rugResolver: Option[RugResolver] = None,
                             loggerOption: Option[Logger] = None)
  extends InstructionRunner {

  private val logger: Logger = loggerOption.getOrElse(LoggerFactory getLogger getClass.getName)

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
            Try {
              fn.run(resolved)
            } match {
              case ScalaSuccess(response) =>
                //ensure the body is String or byte[]!
                val thedata = response.body match {
                  case Some(Body(Some(str), None)) => Some(str)
                  case Some(Body(None, Some(bytes))) => Some(bytes)
                  case Some(Body(_, _)) => throw new BadRugFunctionResponseException(s"Function `${fn.name}` should return a string body or a byte array, but not both")
                  case _ => None
                }
                Response(response.status, response.msg, response.code, thedata)
              case ScalaFailure(throwaball) =>
                val msg = s"Rug Function ${detail.name} threw exception: ${throwaball.getMessage}"
                logger.warn(msg, throwaball)
                Response(Status.Failure, Some(msg), None, Some(throwaball))
            }
          case _ =>
            val msg = s"Cannot find Rug Function ${detail.name}"
            logger.warn(msg)
            Response(Status.Failure, Some(msg), None, None)
        }
      case _ =>
        rugResolver match {
          case Some(resolver) =>
            resolver.resolve(currentRug, extractName(instruction.detail)) match {
              case Some(rug: ProjectGenerator) =>
                doWithProjectName(instruction, (projectName: String) => {
                  val as = projectManagement.generate(rug, parameters, projectName)
                  if (as != null) Response(Success)
                  else Response(Failure, Some(s"failed to run generator ${rug.name} to create $projectName"))
                })
              case Some(rug: ProjectEditor) =>
                doWithProjectName(instruction, (projectName: String) => {
                  projectManagement.edit(rug, parameters, projectName, instruction.detail.editorTarget) match {
                    case _: SuccessfulModification => Response(Success)
                    case success: NoModificationNeeded => Response(Success, Some(success.comment))
                    case failure: FailedModificationAttempt => Response(Failure, Some(failure.failureExplanation))
                  }
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
              case Some(rug) => throw new BadPlanException(s"Unrecognized rug type: $rug")
              case None => throw new BadPlanException(s"Could not find rug with name: ${instruction.detail.name}")
            }
          case _ => throw new IllegalArgumentException(s"Could not find rug with name: ${instruction.detail.name} because no RugResolver supplied")
        }
    }
  }

  /**
    * Convert Instruction.Detail name/coords to a string for resolver
    *
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
