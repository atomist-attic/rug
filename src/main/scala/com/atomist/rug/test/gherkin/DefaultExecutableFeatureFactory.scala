package com.atomist.rug.test.gherkin

import com.atomist.project.archive.{DefaultAtomistConfig, Rugs}
import com.atomist.rug.test.gherkin.handler.command.CommandHandlerFeature
import com.atomist.rug.test.gherkin.handler.event.EventHandlerFeature
import com.atomist.rug.test.gherkin.project.ProjectManipulationFeature
import com.atomist.source.ArtifactSource

/**
  * Default implementation of ExecutableFeatureFactory, which
  * knows about project features and handler features (coming soon), which it identifies
  * by their respective subdirectories under .atomist/test
  */
object DefaultExecutableFeatureFactory extends ExecutableFeatureFactory {

  private val atomistConfig = DefaultAtomistConfig

  override def executableFeatureFor(f: FeatureDefinition,
                                    definitions: Definitions,
                                    rugAs: ArtifactSource,
                                    rugs: Option[Rugs],
                                    listeners: Seq[GherkinExecutionListener]): AbstractExecutableFeature[_] = {
    if (f.definition.path.contains("project"))
      new ProjectManipulationFeature(f, definitions, rugAs, rugs, listeners)
    else if (f.definition.path.contains("handlers/command"))
      new CommandHandlerFeature(f, definitions, rugAs, rugs, listeners)
    else if (f.definition.path.contains("handlers/event"))
      new EventHandlerFeature(f, definitions, rugAs, rugs, listeners)
    else {
      throw new IllegalArgumentException(s"Cannot handle path [${f.definition.path}]: Paths must be of form [${atomistConfig.testsDirectory}/project] or ${atomistConfig.testsDirectory}/handlers]")
    }
  }
}
