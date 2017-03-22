package com.atomist.rug.test.gherkin.handler.command

import com.atomist.project.archive.Rugs
import com.atomist.rug.test.gherkin.{AbstractExecutableFeature, Definitions, FeatureDefinition, GherkinExecutionListener}
import com.atomist.source.ArtifactSource

class CommandHandlerFeature(
                      definition: FeatureDefinition,
                      definitions: Definitions,
                      rugArchive: ArtifactSource,
                      rugs: Option[Rugs],
                      listeners: Seq[GherkinExecutionListener])
  extends AbstractExecutableFeature[CommandHandlerScenarioWorld](definition, definitions, rugs, listeners) {

  override protected def createWorldForScenario: CommandHandlerScenarioWorld = {
    new CommandHandlerScenarioWorld(definitions, rugs)
  }
}
