package com.atomist.rug.test.gherkin

import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.runtime.rugdsl.ReviewContext
import com.atomist.rug.spi.Handlers.Instruction.Review
import com.atomist.source.EmptyArtifactSource

/**
  * Executable feature that manipulates projects
  */
private[gherkin] class ProjectManipulationFeature(
                                          definition: FeatureDefinition,
                                          definitions: Definitions)
  extends AbstractExecutableFeature[ProjectMutableView](definition, definitions) {

  override protected def createFixture = new ProjectMutableView(EmptyArtifactSource())

  override protected def createWorld(fixture: ProjectMutableView): AnyRef = {
    new ProjectWorld
  }
}


class ProjectWorld {

  val reviewContext = new ReviewContext
}