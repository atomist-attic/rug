package com.atomist.rug.test.gherkin

import com.atomist.project.archive.{DefaultAtomistConfig, Rugs}
import com.atomist.rug.test.gherkin.handler.HandlerFeature
import com.atomist.rug.test.gherkin.project.ProjectManipulationFeature
import com.atomist.source.ArtifactSource

/**
  * Default implementation of ExecutableFeatureFactory, which
  * knows about project features and handler features, which it identifies
  * by their respective subdirectories under .atomist/test
  */
object DefaultExecutableFeatureFactory extends ExecutableFeatureFactory {

  private val atomistConfig = DefaultAtomistConfig

  override def executableFeatureFor(f: FeatureDefinition, definitions: Definitions, rugAs: ArtifactSource, rugs: Option[Rugs]): AbstractExecutableFeature[_,_] = {
    if (f.definition.path.contains("project"))
      new ProjectManipulationFeature(f, definitions, rugAs, rugs)
    else if (f.definition.path.contains("handler"))
      new HandlerFeature(f, definitions, rugAs, rugs)
    else {
      throw new IllegalArgumentException(s"Cannot handle path [${f.definition.path}]: Paths must be of form [${atomistConfig.testsDirectory}/project] or ${atomistConfig.testsDirectory}/handlers]")
    }
  }
}
