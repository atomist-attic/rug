package com.atomist.rug.kind.java.spring

import com.atomist.source.{ArtifactSource, FileArtifact}

object GetApplicationPropertiesFile {

  def apply(as: ArtifactSource): Option[FileArtifact] =
    as.findFile(ApplicationPropertiesAssertions.ApplicationPropertiesFilePath)
}

object ApplicationPropertiesAssertions {

  val ApplicationPropertiesFilePath = "src/main/resources/application.properties"

  def hasApplicationProperties(as: ArtifactSource): Boolean =
    as.findFile(ApplicationPropertiesFilePath).isDefined
}