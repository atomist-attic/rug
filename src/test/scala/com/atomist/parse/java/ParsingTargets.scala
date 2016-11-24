package com.atomist.parse.java

import com.atomist.source.file.ClassPathArtifactSource

object ParsingTargets {

  val SpringIoGuidesRestServiceSource = ClassPathArtifactSource.toArtifactSource("spring-parsing-targets/springio-guides-restservice/hello")

  val NewStartSpringIoProject = ClassPathArtifactSource.toArtifactSource("spring-parsing-targets/demo")

  val BuildManagementDependenciesSpringIoProject = ClassPathArtifactSource.toArtifactSource("spring-parsing-targets/buildManagement")

  val NonSpringBootMavenProject = ClassPathArtifactSource.toArtifactSource("editor-targets/non-spring-boot-maven-project")
}
