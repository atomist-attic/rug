package com.atomist.rug.kind.core

import com.atomist.graph.GraphNode
import com.atomist.rug.runtime.js.ExecutionContext
import com.atomist.rug.spi.{ReflectivelyTypedType, Type}
import com.atomist.tree.TreeNode
import com.atomist.tree.utils.NodeUtils

/**
  * Resolves projects from Repos and Commits.
  * Note that this class makes some basic assumptions about Cortex structure:
  * For example, that Repo nodes are tagged "Repo" and have "owner" and "name" keys,
  * and that Commits have a "sha" and attached Repo.
  */
class ProjectType extends Type with ReflectivelyTypedType {

  override def description: String = "Type for a project. Supports global operations. " +
    "Consider using file and other lower types by preference as project" +
    "operations can be inefficient."

  override def runtimeClass: Class[ProjectMutableView] = classOf[ProjectMutableView]

  override def findAllIn(context: GraphNode): Option[Seq[TreeNode]] =
    None

  override def navigate(context: GraphNode, edgeName: String, executionContext: ExecutionContext): Option[Seq[TreeNode]] = {
    val repoResolver = executionContext.repoResolver.getOrElse(
      throw new UnsupportedOperationException(
        s"No repo resolver available: Passed [$executionContext]")
    )

    context match {
      case n if n.hasTag("Repo") =>
        val owner = NodeUtils.requiredKeyValue(context, "owner")
        val name = NodeUtils.requiredKeyValue(context, "name")
        val projectSources =
          if (isSha(edgeName)) repoResolver.resolveSha(owner, name, edgeName)
          else repoResolver.resolveBranch(owner, name, edgeName)
        val pmv = new ProjectMutableView(projectSources)
        Some(Seq(pmv))
      case n if n.hasTag("Commit") =>
        // Commit has a sha and associated Repo
        // We don't care about the edge name, although there should be one
        val sha = NodeUtils.requiredKeyValue(context, "sha")
        val (owner, name) = extractOwnerAndRepoFromAssociatedRepo(context)
        val projectSources = repoResolver.resolveSha(owner, name, sha)
        val pmv = new ProjectMutableView(projectSources)
        Some(Seq(pmv))
      case _ =>
        None
    }
  }

  private def extractOwnerAndRepoFromAssociatedRepo(n: GraphNode): (String,String) = {
    val repo = NodeUtils.requiredNodeOfType(n, "Repo",
      customMessage = Some(s"Commit node must expose Repo to find Project. Did you materialize it with a predicate? Raw node was $n"))
    val owner = NodeUtils.requiredKeyValue(repo, "owner")
    val name = NodeUtils.requiredKeyValue(repo, "name")
    (owner, name)
  }

  private def isSha(s: String) =
    s.matches("[a-fA-F0-9]{40}")
}

object ProjectType {

  /**
    * File containing provenance information in root of edited projects
    */
  val ProvenanceFilePath = ".atomist.yml"
}
