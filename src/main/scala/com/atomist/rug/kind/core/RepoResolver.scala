package com.atomist.rug.kind.core

import com.atomist.source.ArtifactSource

trait RepoResolver {

  /**
    * Resolve the latest in the given branch
    */
  def resolveBranch(owner: String, repoName: String, branch: String = "master"): ArtifactSource

  /**
    * Resolve the tree for this sha
    */
  def resolveSha(owner: String, repoName: String, sha: String): ArtifactSource

}
