package com.atomist.rug.kind.core

import com.atomist.source.ArtifactSource

/**
  * Interface to be implemented by infrastructure that knows how to resolve
  * repos into an ArtifactSource (e.g. from GitHub or a local clone).
  */
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
