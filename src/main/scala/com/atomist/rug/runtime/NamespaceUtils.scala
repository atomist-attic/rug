package com.atomist.rug.runtime

import com.atomist.project.ProjectOperation
import com.atomist.rug.Import

/**
  * Utility methods for working with Rug namespaces.
  */
object NamespaceUtils {

  def namespacePrefix(namespace: Option[String]) = namespace.map(ns => ns + ".").getOrElse("")

  def namespaced(name: String, namespace: Option[String]) = namespacePrefix(namespace) + name

  /**
    * Try to resolve the operation from the given namespace.
    */
  def resolve(name: String,
              namespace: Option[String],
              context: Seq[ProjectOperation],
              imports: Seq[Import] = Nil): Option[ProjectOperation] = {
    // Look in imports
    val theImport = imports.find(_.fqn.endsWith(s".$name"))
    val specificallyImported = theImport.flatMap(imp => context.find(_.name.equals(imp.fqn)))
    specificallyImported.orElse({
      val inOurNamespace = context.find(_.name.equals(namespaced(name, namespace)))
      inOurNamespace
    }).orElse({
      val inDefaultNamespace = context.find(_.name.equals(name))
      inDefaultNamespace
    })
  }
}
