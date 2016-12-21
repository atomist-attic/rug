package com.atomist.parse.java.spring

import com.atomist.project.MaybeFileExtractor
import com.atomist.source.{ArtifactSource, FileArtifact}

object GetApplicationYmlFile extends MaybeFileExtractor {

  override def apply(as: ArtifactSource): Option[FileArtifact] =
    as.findFile(ApplicationYmlAssertions.ApplicationYmlFilePath)
}

object ApplicationYmlAssertions {

  val ApplicationYmlFilePath = "src/main/resources/application.yml"

  def hasApplicationYml(as: ArtifactSource): Boolean =
    as.findFile(ApplicationYmlFilePath).isDefined
}
