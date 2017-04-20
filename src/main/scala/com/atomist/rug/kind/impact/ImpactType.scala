package com.atomist.rug.kind.impact

import com.atomist.graph.GraphNode
import com.atomist.rug.kind.core.ProjectType
import com.atomist.rug.runtime.js.ExecutionContext
import com.atomist.rug.spi.{ReflectivelyTypedType, Type}
import com.atomist.tree.TreeNode
import com.atomist.tree.utils.NodeUtils

class ImpactType extends Type with ReflectivelyTypedType {

  import NodeUtils._
  import ProjectType.extractOwnerAndRepoFromAssociatedRepo

  override def description: String = "Represents an impact of a change"

  override def runtimeClass: Class[Impact] = classOf[Impact]

  override def findAllIn(context: GraphNode): Option[Seq[TreeNode]] =
    None

  override def navigate(context: GraphNode, edgeName: String, executionContext: ExecutionContext): Option[Seq[Impact]] = {
    val repoResolver = executionContext.repoResolver.getOrElse(
      throw new UnsupportedOperationException(
        s"No repo resolver available: Passed [$executionContext]")
    )

    context match {
      case push if push.hasTag("Push") =>
        // Push has before and after commits and an associated repo
        // We don't care about the edge name
        val beforeCommit = requiredKey(push, "before")
        val afterCommit = requiredKey(push, "after")
        val beforeSha = requiredKeyValue(beforeCommit, "sha")
        val afterSha = requiredKeyValue(afterCommit, "sha")

        val (owner, name) = extractOwnerAndRepoFromAssociatedRepo(context)
        val beforeSources = repoResolver.resolveSha(owner, name, beforeSha)
        val afterSources = repoResolver.resolveSha(owner, name, afterSha)
        Some(Seq(new Impact(push, beforeSources, afterSources)))
      case _ =>
        None
    }
  }
}
