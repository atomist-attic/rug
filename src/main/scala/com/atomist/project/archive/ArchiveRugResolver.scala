package com.atomist.project.archive

import com.atomist.rug.DuplicateRugException
import com.atomist.rug.runtime._
import com.atomist.rug.runtime.js.JavaScriptContext.EngineInitializer
import com.atomist.rug.runtime.js.nashorn.NashornJavaScriptEngine
import com.atomist.rug.runtime.js.{JavaScriptEngineContextFactory, _}
import com.atomist.source.ArtifactSource

/**
  * Used by PlanRunner and Project Operations to find Rugs
  *
  * We explicitly do _not_ support loading a Rug that is not a direct dependency
  * i.e. you cannot load a Rug that is brought in transitively
  *
  * A None means no resolution!
  */
class ArchiveRugResolver(graph: Dependency,
                         engineInitializer: EngineInitializer = NashornJavaScriptEngine.redirectConsoleToSysOut) extends RugResolver {

  private val finders: Seq[JavaScriptRugFinder[_ <: Rug]] = Seq(
    new JavaScriptCommandHandlerFinder(),
    new JavaScriptResponseHandlerFinder(),
    new JavaScriptEventHandlerFinder(),
    new JavaScriptProjectGeneratorFinder(),
    new JavaScriptProjectEditorFinder())

  /**
    * Find Rugs of all known kinds in the artifact.
    */
  private def find(as: ArtifactSource, resolver: RugResolver): Seq[Rug] = {
    val jsc = JavaScriptEngineContextFactory.create(as)
    val found = finders.flatMap(finder => finder.find(jsc, Some(resolver)))
    val grouped = found.groupBy(_.name).collect{case x if x._2.size > 1 => x._1}
    if(grouped.nonEmpty){
      throw new DuplicateRugException(s"The following rugs have duplicates in the archive: ${grouped.mkString(", ")}", found)
    }else{
      found
    }
  }

  //group:artifact:name
  private def fqRex = "^(.*?):(.*?):(.*?)$".r

  /**
    * Top of the dep graph.
    */
  override val resolvedDependencies: ResolvedDependency = {
    val rugs = find(graph.source, this)
    ResolvedDependency(graph.source, graph.address, dependencies(graph.dependencies), rugs)
  }

  /**
    * Recursively resolve all the rugs.
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
    * Search direct dependencies of root for a rug.
    */
  override def resolve(root: Rug, nameOrFqName: String): Option[Rug] = {
    findResolvedDependency(root) match {
      case Some(rootDep) =>
        val local = rootDep.resolvedRugs.find(p => rugMatches(p, rootDep, nameOrFqName))
        if (local.nonEmpty) {
          local
        } else {
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
    */
  private def rugMatches(rug: Rug, rootDep: ResolvedDependency, name: String): Boolean = {
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
    * Find the node in the dependency graph for a given Rug instance.
    */
  override def findResolvedDependency(rug: Rug): Option[ResolvedDependency] = {
    findRug(rug, resolvedDependencies)
  }

  /**
    * Find the root rug in the graph.
    */
  private def findRug(root: Rug, resolved: ResolvedDependency): Option[ResolvedDependency] = {
    resolved.resolvedRugs.find(r => r == root) match {
      case Some(_) => Some(resolved)
      case _ =>
        val matched = resolved.dependencies.map(findRug(root, _)).flatten
        if (matched.isEmpty) {
          None
        }
        else {
          Option(matched.head)
        }
    }
  }
}
