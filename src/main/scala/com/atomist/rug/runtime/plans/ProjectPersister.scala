package com.atomist.rug.runtime.plans

import com.atomist.source.ArtifactSource

/**
  * Save the output from a Generator
  */
trait ProjectPersister[T <: ArtifactSource] {
  //should throw exceptions if persistence not possible!
  def persist(projectName: String, source: ArtifactSource): T
}
