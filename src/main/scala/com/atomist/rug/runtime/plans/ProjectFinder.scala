package com.atomist.rug.runtime.plans

import com.atomist.source.ArtifactSource

/**
  * Used by cli/runner etc to find a Project given its name, and presumably some other context
  *
  * Editors/Reviewers can be run on the result
  */
trait ProjectFinder {
  def findArtifactSource(project_name: String) : Option[ArtifactSource]
}
