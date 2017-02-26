package com.atomist.rug.test.gherkin

import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.source.EmptyArtifactSource

/**
  * Executable feature that manipulates projects
  */
private[gherkin] class ProjectManipulationFeature(
                                          definition: FeatureDefinition,
                                          definitions: Definitions)
  extends AbstractExecutableFeature[ProjectMutableView](definition, definitions) {

  override protected def createTarget = new ProjectMutableView(EmptyArtifactSource())
}
