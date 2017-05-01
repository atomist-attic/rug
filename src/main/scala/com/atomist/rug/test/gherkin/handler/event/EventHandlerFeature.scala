package com.atomist.rug.test.gherkin.handler.event

import com.atomist.project.archive.Rugs
import com.atomist.rug.test.gherkin._
import com.atomist.source.ArtifactSource

class EventHandlerFeature(
                      definition: FeatureDefinition,
                      definitions: Definitions,
                      rugArchive: ArtifactSource,
                      rugs: Option[Rugs],
                      listeners: Seq[GherkinExecutionListener],
                      config: GherkinRunnerConfig)
  extends AbstractExecutableFeature[EventHandlerScenarioWorld](definition, definitions, rugs, listeners, config) {

  override protected def createWorldForScenario(): EventHandlerScenarioWorld = {
    new EventHandlerScenarioWorld(definitions, rugs, listeners, config)
  }
}
