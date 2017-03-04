package com.atomist.parse.java

import com.atomist.source.ArtifactSource
import com.atomist.source.file.ClassPathArtifactSource

object ParsingTargets {

  val SpringIoGuidesRestServiceSource: ArtifactSource = ClassPathArtifactSource.toArtifactSource("spring-parsing-targets/springio-guides-restservice/hello")

  val NewStartSpringIoProject: ArtifactSource = ClassPathArtifactSource.toArtifactSource("spring-parsing-targets/demo")

  val BuildManagementDependenciesSpringIoProject: ArtifactSource = ClassPathArtifactSource.toArtifactSource("spring-parsing-targets/buildManagement")

  val NonSpringBootMavenProject: ArtifactSource = ClassPathArtifactSource.toArtifactSource("editor-targets/non-spring-boot-maven-project")

  val MultiPomProject: ArtifactSource = ClassPathArtifactSource.toArtifactSource("spring-parsing-targets")

}
