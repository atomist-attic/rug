package com.atomist.rug.runtime.plans

import com.atomist.param.SimpleParameterValues
import com.atomist.project.edit.ProjectEditor
import com.atomist.project.generate.ProjectGenerator
import com.atomist.project.review.ProjectReviewer
import com.atomist.rug.runtime._
import com.atomist.rug.spi.Handlers.Instruction._
import com.atomist.rug.spi.Handlers.Status.{Failure, Success}
import com.atomist.rug.spi.Handlers.{Instruction, Response}
import com.atomist.rug.spi.RugFunctionRegistry
import com.atomist.source.ArtifactSource

/**
  * Run instructions synchronously in this JVM
  *
  * TODO - ensure we blow up if there are rugs or instructions with duplicate names
  * and don't have different GA's
  */
class LocalInstructionRunner(rugs: Seq[AddressableRug],
                             projectFinder: ProjectFinder,
                             projectPersister: ProjectPersister[ArtifactSource],
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

  private def doWithProject(instruction: Instruction, action: (ArtifactSource) => Response) = {
    doWithProjectName(
      instruction,
      (projectName: String) => {
        projectFinder.findArtifactSource(projectName) match {
          case Some(project) => action(project)
          case _ => Response(Failure, None, None, Some(s"Project '$projectName' could not be found."))
        }
      }
    )
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
              val newProject = rug.generate(projectName, parameters)
              val persistAttempt = projectPersister.persist(projectName, newProject)
              Response(Success, None, None, Some(persistAttempt))
            })
          case Some(rug: ProjectEditor) =>
            doWithProject(instruction, (project: ArtifactSource) => {
              val modificationAttempt = rug.modify(project, parameters)
              Response(Success, None, None, Some(modificationAttempt))
            })
          case Some(rug: ProjectReviewer) =>
            doWithProject(instruction, (project: ArtifactSource) => {
              val reviewResult = rug.review(project, parameters)
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
