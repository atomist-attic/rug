package com.atomist.rug.test.gherkin

import com.atomist.project.archive.Rugs
import com.atomist.source.ArtifactSource

/**
  * Make an executable feature from this definition
  */
trait ExecutableFeatureFactory {

  def executableFeatureFor(f: FeatureDefinition,
                           definitions: Definitions,
                           rugAs: ArtifactSource,
                           rugs: Option[Rugs],
                           listeners: Seq[GherkinExecutionListener]): AbstractExecutableFeature[_]
}


