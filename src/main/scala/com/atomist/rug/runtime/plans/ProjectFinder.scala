package com.atomist.rug.runtime.plans

import com.atomist.param.ParameterValues
import com.atomist.project.ProjectOperation
import com.atomist.source.ArtifactSource

/**
  * Used by cli/runner etc to find a Project given its name, and presumably some other context
  *
  * Editors/Reviewers can be run on the result
  */
trait ProjectFinder {
  def findArtifactSource(editor: ProjectOperation, arguments: ParameterValues, projectName: String) : Option[ArtifactSource]
}
