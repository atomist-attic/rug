package com.atomist.rug.runtime.plans

import com.atomist.param.ParameterValues
import com.atomist.project.generate.ProjectGenerator
import com.atomist.source.ArtifactSource

/**
  * Save the output from a Generator
  */
trait ProjectPersister[T <: ArtifactSource] {
  //should throw exceptions if persistence not possible!
  def persist(generator: ProjectGenerator, arguments: ParameterValues, projectName: String, source: ArtifactSource): T
}
