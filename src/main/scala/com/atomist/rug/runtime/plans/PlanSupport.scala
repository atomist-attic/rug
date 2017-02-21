package com.atomist.rug.runtime.plans

import com.atomist.project.edit.ProjectEditor
import com.atomist.project.generate.ProjectGenerator
import com.atomist.project.review.ProjectReviewer
import com.atomist.rug.runtime.{AddressableRug, CommandHandler, ResponseHandler}
import com.atomist.rug.spi.Handlers.Instruction
import com.atomist.rug.spi.Handlers.Instruction._

/**
  * Some handy stuff for matching up Rugs
  */
trait PlanSupport {

  def matches(rug: AddressableRug, instruction: Instruction): Boolean = {
    val typeMatches = (instruction, rug) match {
      case (i: Edit, r: ProjectEditor) => true
      case (i: Generate, r: ProjectGenerator) => true
      case (i: Review, r: ProjectReviewer) => true
      case (i: Command, r: CommandHandler) => true
      case (i: Respond, r: ResponseHandler) => true
      case _ => false
    }
    typeMatches && instruction.detail.name == rug.name &&
      instruction.detail.coordinates.exists(coords => rug.group == coords.group && rug.artifact == coords.artifact)
  }

  def findMatch(rugs: Seq[AddressableRug], instruction: Instruction): Option[AddressableRug] = {
    rugs.find(p => matches(p, instruction))
  }
}
