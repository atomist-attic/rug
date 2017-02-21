package com.atomist.parse.java.spring

import com.atomist.source.ArtifactSource

object ApplicationYamlAssertions {

  val ApplicationYamlFilePath = "src/main/resources/application.yml"

  def hasApplicationYaml(as: ArtifactSource): Boolean =
    as.findFile(ApplicationYamlFilePath).isDefined
}
