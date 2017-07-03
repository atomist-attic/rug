package com.atomist.rug.runtime.js.interop

import com.atomist.rug.kind.core.{ProjectMutableView, RepoResolver}
import com.atomist.rug.spi.ExportFunction

/**
  * Backs the TypeScript GitProjectLoader interface.
  */
class jsGitProjectLoader(rr: Option[RepoResolver]) {

  /**
    * Resolve the latest in the given branch. Throw exception if unable to.
    */
  @ExportFunction(readOnly = true, description = "Load a specific branch")
  def loadBranch(owner: String, repoName: String, branch: String): ProjectMutableView = {
    val as = rr.map(_.resolveBranch(owner, repoName, branch)).getOrElse(
      throw new IllegalStateException(s"No RepoResolver available to resolve $owner:$repoName:branch=$branch")
    )
    new ProjectMutableView(originalBackingObject = as)
  }

  /**
    * Resolve the tree for this sha. Throw exception if unable to.
    */
  @ExportFunction(readOnly = true, description = "Load a specific sha")
  def loadSha(owner: String, repoName: String, sha: String): ProjectMutableView = {
    val as = rr.map(_.resolveSha(owner, repoName, sha)).getOrElse(
      throw new IllegalStateException(s"No RepoResolver available to resolve $owner:$repoName:sha=$sha")
    )
    new ProjectMutableView(originalBackingObject = as)
  }
}
