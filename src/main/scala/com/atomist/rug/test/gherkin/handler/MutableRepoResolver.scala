package com.atomist.rug.test.gherkin.handler

import com.atomist.rug.kind.core.RepoResolver
import com.atomist.source.ArtifactSource

/**
  * Used in testing infrastructure and the test suite
  */
class MutableRepoResolver extends RepoResolver {

  private case class ProjectId(owner: String, repoName: String, sha: String)

  private var definedRepos: Map[ProjectId, ArtifactSource] = Map()

  def defineRepo(owner: String, name: String, branchOrSha: String, as: ArtifactSource): Unit = {
    definedRepos += (ProjectId(owner, name, branchOrSha) -> as)
  }

  override def resolveBranch(owner: String, repoName: String, branch: String): ArtifactSource = {
    val pid = ProjectId(owner, repoName, branch)
    definedRepos.getOrElse(ProjectId(owner, repoName, branch),
      throw new IllegalArgumentException(s"Repo not found by branch: [$pid]: Known repos are ${definedRepos.keySet}")
    )
  }

  override def resolveSha(owner: String, repoName: String, sha: String): ArtifactSource = {
    val pid = ProjectId(owner, repoName, sha)
    definedRepos.getOrElse(ProjectId(owner, repoName, sha),
      throw new IllegalArgumentException(s"Repo not found by sha: [$pid]; Known repos are ${definedRepos.keySet}")
    )
  }

}
