package com.atomist.tree.marshal

import com.atomist.rug.kind.core.ProjectType
import com.atomist.source.ArtifactSource

/**
  * Resolves source control repositories into projects
  */
trait RepoResolver {

  /**
    * Take an unresolvable project node and return the artifact source that it represents
    * @param unresolvableProjectNode  the project node to resolve
    * @return ArtifactSource
    */
  def resolveRepoToProject(unresolvableProjectNode: UnresolvableProjectNode): ArtifactSource

  // TODO - we can convert the artifact source to a ProjectMutableView using it's single arg constructor
  //    github lib seems able to return an implementation of ArtifactSource for a repo
  //    we can then execute path expressions on that e.g. `ee.evaluate(pmv, expr, DefaultTypeRegistry)`
  // see PathExpressionsAgainstProjectTest.scala
}
