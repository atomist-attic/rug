package com.atomist.rug.test.gherkin.project

import com.atomist.project.archive.Rugs
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.test.gherkin._
import com.atomist.source.{ArtifactSource, EmptyArtifactSource}

/**
  * Executable feature that manipulates projects
  */
class ProjectManipulationFeature(
                                  definition: FeatureDefinition,
                                  definitions: Definitions,
                                  rugArchive: ArtifactSource,
                                  rugs: Option[Rugs],
                                  listeners: Seq[GherkinExecutionListener] = Nil)
  extends AbstractExecutableFeature[ProjectScenarioWorld](definition, definitions, rugs, listeners) {

  override protected def createWorldForScenario: ScenarioWorld = {
    new ProjectScenarioWorld(definitions, rugs)
  }
}
