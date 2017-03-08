package com.atomist.tree.marshal

import com.atomist.rug.kind.core.ProjectType
import com.atomist.source.ArtifactSource

/**
  * Resolves source control repositories into projects
  */
trait RepoResolver {

  /**
    * Take an unresolvable
    * @param unresolvableProjectNode
    * @return
    */
  def resolveRepoToProject(unresolvableProjectNode: UnresolvableProjectNode): ArtifactSource
  // TODO - we can convert the artifact source to a ProjectMutableView using it's single arg constructor
  //    github lib seems able to return an implementation of ArtifactSource for a repo
  //    we can then execute path expressions on that e.g. `ee.evaluate(pmv, expr, DefaultTypeRegistry)`
  // see PathExpressionsAgainstProjectTest.scala
  // TODO - lets also fix being able to handle multiple nodes of the same type by creating a set whilst we are in here
  // TODO - maybe pass this interface the actual useful context rather than the project node itself
}
