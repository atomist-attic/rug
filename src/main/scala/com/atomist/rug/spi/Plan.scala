package com.atomist.rug.spi

import com.atomist.param.ParameterValue
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.tree.TreeNode

/**
  * Beans that map to the @atomist/rug/operations/operations/Handlers
  */

object Plan {



  case class Plan(messages: Seq[Message], rugs: Seq[Rug])

  object Plan {
    def build(fromNashon: Any) : Option[Plan] = {
      Some(Plan(Nil,Nil))
    }
  }

  case class Message(body: MessageBody,
                     rugs: Seq[Rug],
                     node: Option[TreeNode],
                     channelId: Option[String])

  abstract class Rug(name: String,
                 kind: RugKind,
                 params: Option[Seq[ParameterValue]],
                 coordinates: Option[MavenCoordinate],
                 success: Option[Callback],
                 failure: Option[Callback])

  case class ProjectRug(project: ProjectMutableView,
                        name: String,
                        kind: RugKind,
                        params: Option[Seq[ParameterValue]],
                        coordinates: Option[MavenCoordinate],
                        success: Option[Callback],
                        failure: Option[Callback])
    extends Rug(name,kind,params,coordinates ,success,failure)

  case class Callback(function: String, params: Option[Seq[ParameterValue]])
}


sealed trait RugKind

object RugKind extends Enumeration {
  val Editor = Value("editor")
  val Generator = Value("generator")
  val Reviewer = Value("reviewer")
  val Execution = Value("execution")
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