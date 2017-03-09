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
                                  rugs: Option[Rugs] = None,
                                  listeners: Seq[GherkinExecutionListener] = Nil)
  extends AbstractExecutableFeature[ProjectMutableView, ProjectScenarioWorld](definition, definitions, listeners) {

  override protected def createFixture =
    new ProjectMutableView(rugAs = rugArchive, originalBackingObject = EmptyArtifactSource())

  override protected def createWorldForScenario(fixture: ProjectMutableView): ScenarioWorld = {
    new ProjectScenarioWorld(definitions, fixture, rugs)
  }
}
