package com.atomist.rug.kind.java.spring

import com.atomist.project.{MaybeFileExtractor, ProjectAssertion}
import com.atomist.source.{ArtifactSource, FileArtifact}

object GetApplicationPropertiesFile extends MaybeFileExtractor {

  override def apply(as: ArtifactSource): Option[FileArtifact] =
    as.findFile(HasApplicationProperties.ApplicationPropertiesFilePath)
}

object HasApplicationProperties extends ProjectAssertion {

  val ApplicationPropertiesFilePath = "src/main/resources/application.properties"

  override def apply(as: ArtifactSource): Boolean =
    as.findFile(ApplicationPropertiesFilePath).isDefined
}