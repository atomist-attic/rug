package com.atomist.project.predicate

import com.atomist.project.{ProjectOperation, ProjectOperationArguments}
import com.atomist.source.ArtifactSource

trait ProjectPredicate extends ProjectOperation {

  def holds(as: ArtifactSource, poa: ProjectOperationArguments): Boolean

}
