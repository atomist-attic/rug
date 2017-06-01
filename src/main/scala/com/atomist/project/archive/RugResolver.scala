package com.atomist.project.archive
import com.atomist.project.edit.ProjectEditor
import com.atomist.project.generate.ProjectGenerator
import com.atomist.rug.runtime.{CommandHandler, EventHandler, ResponseHandler, Rug}
import com.atomist.source.ArtifactSource

trait RugResolver {

  /**
    * Returns the root.
    */
  val resolvedDependencies: ResolvedDependency

  /**
    * Search direct dependencies of root for a rug.
    */
  def resolve(root: Rug, nameOrFqName: String): Option[Rug]

  /**
    * Find the node in the dependency graph for a given Rug instance.
    */
  def findResolvedDependency(rug: Rug): Option[ResolvedDependency]
}

sealed trait RugDependency[T <: RugDependency[T]] {
  def address: Option[Coordinate]

  def source: ArtifactSource

  def dependencies: Seq[T]
}

case class Dependency(override val source: ArtifactSource,
                      override val address: Option[Coordinate] = None,
                      override val dependencies: Seq[Dependency] = Nil)
  extends RugDependency[Dependency]

case class ResolvedDependency(override val source: ArtifactSource,
                              override val address: Option[Coordinate] = None,
                              override val dependencies: Seq[ResolvedDependency],
                              resolvedRugs: Seq[Rug])
  extends RugDependency[ResolvedDependency] {

  def rugs: Rugs = {
    Rugs(
      resolvedRugs.collect { case h: ProjectEditor => h },
      resolvedRugs.collect { case h: ProjectGenerator => h },
      resolvedRugs.collect { case h: CommandHandler => h },
      resolvedRugs.collect { case h: EventHandler => h },
      resolvedRugs.collect { case h: ResponseHandler => h })
  }
}

case class Coordinate(group: String, artifact: String, version: String)
