package com.atomist.parse.java.spring

import com.atomist.project.{MaybeFileExtractor, ProjectAssertion}
import com.atomist.source.{ArtifactSource, FileArtifact}

object GetApplicationYmlFile extends MaybeFileExtractor {

  override def apply(as: ArtifactSource): Option[FileArtifact] =
    as.findFile(HasApplicationYml.ApplicationYmlFilePath)
}

object HasApplicationYml extends ProjectAssertion {

  val ApplicationYmlFilePath = "src/main/resources/application.yml"

  override def apply(as: ArtifactSource): Boolean =
    as.findFile(ApplicationYmlFilePath).isDefined
}
