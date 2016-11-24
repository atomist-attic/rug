package com.atomist.project.generate

import com.atomist.project.common.InvalidParametersException
import com.atomist.project.{ProjectDelta, ProjectOperationArguments}
import com.atomist.source.ArtifactSource

/**
  * Implemented by classes that can generate projects,
  * given parameter values, which should match
  * parameters specified in the parameters() method.
  */
trait ProjectGenerator extends ProjectDelta {

  @throws(classOf[InvalidParametersException])
  def generate(tcc: ProjectOperationArguments): ArtifactSource
}
