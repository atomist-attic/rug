package com.atomist.rug.kind.core

import com.atomist.source.ArtifactSource
import org.scalatest.Matchers

case class FixedBranchRepoResolver(expectedOwner: String, expectedRepo: String, expectedBranch: String, as: ArtifactSource)
  extends RepoResolver with Matchers {

  override def resolveSha(owner: String, repoName: String, sha: String): ArtifactSource =
    fail(s"Can't resolve repo from sha")

  override def resolveBranch(owner: String, repoName: String, branch: String): ArtifactSource = {
    assert(owner === expectedOwner)
    assert(repoName === expectedRepo)
    assert(branch === expectedBranch)
    as
  }

}