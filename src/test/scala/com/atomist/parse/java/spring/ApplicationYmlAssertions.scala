package com.atomist.parse.java.spring

import com.atomist.source.ArtifactSource

object ApplicationYmlAssertions {

  val ApplicationYmlFilePath = "src/main/resources/application.yml"

  def hasApplicationYml(as: ArtifactSource): Boolean =
    as.findFile(ApplicationYmlFilePath).isDefined
}
