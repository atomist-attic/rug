package com.atomist.rug.spi

import com.atomist.param.ParameterValue
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.tree.TreeNode

/**
  * Beans that map to the @atomist/rug/operations/operations/Handlers
  */

object Plan {

  case class Plan(messages: Seq[Message], instructions: Seq[Instruction])

  case class Message(text: Option[String],
                      body: Option[JsonBody],
                      channelId: Option[String],
                      node: Option[TreeNode],
                      instructions: Seq[Instruction])

  sealed trait Instruction

  class Rug(name: String,
                     coordinates: Option[MavenCoordinate],
                      parameters: Option[Seq[ParameterValue]],
                      kind: InstructionKind,
                      label: Option[String],
                      onSuccess: Option[Callback],
                      onFailure: Option[Callback]) extends Instruction

  case class ProjectRug(project: ProjectMutableView, rug: Rug) extends Instruction

  case class Callback(function: String, params: Option[Seq[ParameterValue]])
}


sealed trait InstructionKind

object InstructionKind extends Enumeration {
  val Generate = Value("generate")
  val Edit = Value("edit")
  val Review = Value("review")
  val Execute = Value("execute")
  val Respond = Value("respond")
  val Command = Value("command")
}

trait MavenCoordinate {
  val group: String
  val artifact: String
  val version: String
}

trait JsonBody extends MessageBody {
  val json: String
}

trait MessageText extends MessageBody {
  val text: String
}

trait MessageBody