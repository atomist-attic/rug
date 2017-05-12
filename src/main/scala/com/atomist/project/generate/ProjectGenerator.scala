package com.atomist.project.generate

import com.atomist.param.ParameterValues
import com.atomist.project.ProjectOperation
import com.atomist.project.common.InvalidParametersException
import com.atomist.rug.kind.core.ProjectContext
import com.atomist.rug.runtime.js.LocalRugContext
import com.atomist.source.ArtifactSource

/**
  * Implemented by classes that can generate projects,
  * given parameter values, which should match
  * parameters specified in the parameters() method.
  */
trait ProjectGenerator
  extends ProjectOperation {

  @throws(classOf[InvalidParametersException])
  def generate(projectName: String, pvs: ParameterValues, ProjectContext: ProjectContext = new ProjectContext(LocalRugContext)): ArtifactSource
}
