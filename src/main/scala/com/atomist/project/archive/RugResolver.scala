package com.atomist.project.archive

import com.atomist.project.edit.ProjectEditor
import com.atomist.project.generate.ProjectGenerator
import com.atomist.project.review.ProjectReviewer
import com.atomist.rug.runtime._
import com.atomist.rug.runtime.js._
import com.atomist.source.ArtifactSource

/**
  * Used by PlanRunner and Project Operations to find Rugs
  *
  * We explicitly do _not_ support loading a Rug that is not a direct dependency
  * i.e. you cannot load a Rug that is brought in transitively
  *
  * A None means no resolution!
  */
class RugResolver(graph: Dependency) {

  private val finders: Seq[JavaScriptRugFinder[_ <: Rug]] = Seq(
    new JavaScriptCommandHandlerFinder(),
    new JavaScriptResponseHandlerFinder(),
    new JavaScriptEventHandlerFinder(),
    new JavaScriptProjectReviewerFinder(),
    new JavaScriptProjectGeneratorFinder(),
    new JavaScriptProjectEditorFinder())

  /**
    * Find Rugs of all known kinds in the artifact
    *
    * @param as
    * @return
    */
  private def find(as: ArtifactSource, resolver: RugResolver): Seq[Rug] = {
    val jsc = new JavaScriptContext(as)
    finders.flatMap(finder => finder.find(jsc, Some(resolver)))
  }

  //group:artifact:name
  private def fqRex = "^(.*?):(.*?):(.*?)$".r

  /**
    * Top of the dep graph.
    */
  val resolvedDependencies: ResolvedDependency = {
    val rugs = find(graph.source, this)
    ResolvedDependency(graph.source, graph.address, dependencies(graph.dependencies), rugs)
  }

  /**
    * Recursively resolve all the rugs
    *
    * @param deps
    * @return
    */
  private def dependencies(deps: Seq[Dependency]): Seq[ResolvedDependency] = {
    deps.map(dep =>
      if (dep.dependencies.nonEmpty) {
        ResolvedDependency(dep.source, dep.address, dependencies(dep.dependencies), find(dep.source, this))
      } else {
        ResolvedDependency(dep.source, dep.address, Nil, find(dep.source, this))
      }
    )
  }

  /**
    * Search direct dependencies of root for a rug
    *
    * @param root
    * @param nameOrFqName
    * @return
    */
  def resolve(root: Rug, nameOrFqName: String): Option[Rug] = {
    findRug(root, resolvedDependencies) match {
      case Some(rootDep) =>
        val local = rootDep.resolvedRugs.find(p => rugMatches(p, rootDep, nameOrFqName))
        if(local.nonEmpty){
          local
        }else{
          (for {
            dep <- rootDep.dependencies
            rug <- dep.resolvedRugs
            if rugMatches(rug, dep, nameOrFqName)
          } yield rug).headOption
        }
      case _ => None
    }
  }

  /**
    * Does the current rug/dep match the name?
    * @param rug
    * @param rootDep
    * @param name
    * @return
    */
  private def rugMatches(rug: Rug, rootDep: ResolvedDependency, name: String) : Boolean = {
    name match {
      case simple: String if !simple.contains(":") => simple == rug.name
      case fq: String => (fqRex.findFirstMatchIn(fq), rootDep.address) match {
        case (Some(m), Some(address)) =>
            m.group(1) == address.group &&
            m.group(2) == address.artifact &&
            m.group(3) == rug.name
        case _ => false
      }
    }
  }

  /**
    * Find the root rug in the graph
    *
    * @param root
    * @return
    */
  private def findRug(root: Rug, resolved: ResolvedDependency): Option[ResolvedDependency] = {
    resolved.resolvedRugs.find(r => r == root) match {
      case Some(rug) => Some(resolved)
      case _ =>
        val matched = resolved.dependencies.map(findRug(root, _))
        if (matched.isEmpty) {
          None
        } else {
          matched.head
        }
    }
  }
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
      resolvedRugs.collect { case h: ProjectReviewer => h },
      resolvedRugs.collect { case h: CommandHandler => h },
      resolvedRugs.collect { case h: EventHandler => h },
      resolvedRugs.collect { case h: ResponseHandler => h })
  }
}

case class Coordinate(group: String, artifact: String, version: String)
