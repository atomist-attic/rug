package com.atomist.rug.kind.core

import com.atomist.source.ArtifactSource
import org.scalatest.Matchers

case class FixedShaRepoResolver(expectedOwner: String, expectedRepo: String, expectedSha: String, as: ArtifactSource)
  extends RepoResolver with Matchers {

  override def resolveBranch(owner: String, repoName: String, branch: String): ArtifactSource =
    fail(s"Can't resolve repo from branch")

  override def resolveSha(owner: String, repoName: String, sha: String): ArtifactSource = {
    assert(owner === expectedOwner)
    assert(repoName === expectedRepo)
    assert(sha === expectedSha)
    as
  }
}
