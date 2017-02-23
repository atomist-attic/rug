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

    val coordsMatch = instruction.detail.coordinates match {
      case Some(c) => rug.group == c.group && rug.artifact == c.artifact
      case _ => true
    }

    val nameMatches = instruction.detail.name == rug.name

    typeMatches &&  coordsMatch && nameMatches
  }

  def findMatch(rugs: Seq[AddressableRug], instruction: Instruction): Option[AddressableRug] = {
    rugs.find(p => matches(p, instruction))
  }
}
