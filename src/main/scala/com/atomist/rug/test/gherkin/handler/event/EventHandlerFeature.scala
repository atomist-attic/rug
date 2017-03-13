package com.atomist.rug.test.gherkin.handler.event

import com.atomist.project.archive.Rugs
import com.atomist.rug.runtime.js.RugContext
import com.atomist.rug.test.gherkin.handler.FakeRugContext
import com.atomist.rug.test.gherkin.{AbstractExecutableFeature, Definitions, FeatureDefinition, GherkinExecutionListener}
import com.atomist.source.ArtifactSource
import com.atomist.tree.IdentityTreeMaterializer

class EventHandlerFeature(
                      definition: FeatureDefinition,
                      definitions: Definitions,
                      rugArchive: ArtifactSource,
                      rugs: Option[Rugs],
                      listeners: Seq[GherkinExecutionListener])
  extends AbstractExecutableFeature[EventHandlerScenarioWorld](definition, definitions, rugs, listeners) {

  override protected def createWorldForScenario: EventHandlerScenarioWorld = {
    new EventHandlerScenarioWorld(definitions, rugs)
  }
}




