package com.atomist.rug.kind.core

import com.atomist.graph.GraphNode
import com.atomist.rug.runtime.js.ExecutionContext
import com.atomist.rug.spi.{ReflectivelyTypedType, Type}
import com.atomist.tree.TreeNode
import com.atomist.tree.utils.NodeUtils

/**
  * Resolves projects from Repos
  */
class ProjectType
  extends Type
    with ReflectivelyTypedType {

  override def description: String = "Type for a project. Supports global operations. " +
    "Consider using file and other lower types by preference as project" +
    "operations can be inefficient."

  override def runtimeClass: Class[ProjectMutableView] = classOf[ProjectMutableView]

  override def findAllIn(context: GraphNode): Option[Seq[TreeNode]] =
    None

  override def navigate(context: GraphNode, edgeName: String, executionContext: ExecutionContext): Option[Seq[TreeNode]] = context match {
    case n if n.nodeTags.contains("Repo") =>
      //println(s"Looking for project in Repo node via $branch: $n")
      val owner = NodeUtils.requiredKeyValue(context, "owner")
      val name = NodeUtils.requiredKeyValue(context, "name")
      val repoResolver = executionContext.repoResolver.getOrElse(
        throw new UnsupportedOperationException(
          s"No repo resolver available: Passed [$executionContext]")
      )
      val projectSources =
        if (isSha(edgeName)) repoResolver.resolveSha(owner, name, edgeName)
        else repoResolver.resolveBranch(owner, name, edgeName)
      val pmv = new ProjectMutableView(projectSources)
      Some(Seq(pmv))
    case _ =>
      None
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
