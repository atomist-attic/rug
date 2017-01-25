package com.atomist.project.predicate

import com.atomist.param.ParameterValues
import com.atomist.project.ProjectOperation
import com.atomist.source.ArtifactSource

trait ProjectPredicate extends ProjectOperation {

  def holds(as: ArtifactSource, poa: ParameterValues): Boolean

}
