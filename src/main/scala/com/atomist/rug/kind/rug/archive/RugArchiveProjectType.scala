package com.atomist.rug.kind.rug.archive

import com.atomist.rug.runtime.rugdsl.DefaultEvaluator
import com.atomist.rug.spi.{ReflectivelyTypedType, Type}

class RugArchiveProjectType
  extends Type(DefaultEvaluator)
  with ReflectivelyTypedType {
  def viewManifest: Manifest[_] = manifest[RugArchiveProjectMutableView]

  // Members declared in com.atomist.rug.spi.Typed
  def description: String = ???

  // Members declared in com.atomist.rug.kind.dynamic.ViewFinder
  protected def findAllIn(rugAs: com.atomist.source.ArtifactSource,selected: com.atomist.rug.parser.Selected,context: com.atomist.rug.spi.MutableView[_],poa: com.atomist.project.ProjectOperationArguments,identifierMap: Map[String,Object]): Option[Seq[com.atomist.rug.spi.MutableView[_]]] = ???
  class RugArchiveProjectType
}
