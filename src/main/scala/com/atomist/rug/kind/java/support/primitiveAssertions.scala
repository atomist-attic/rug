package com.atomist.rug.kind.java.support

import com.atomist.source.ArtifactSource
import com.atomist.util.lang.{JavaHelpers, MavenConstants}

object JavaAssertions {

  val isJava: ArtifactSource => Boolean = project =>
    project.allFiles.exists(JavaHelpers.isJavaSourceArtifact(_))

  val isMaven: ArtifactSource => Boolean =
    project => project.findFile(MavenConstants.PomPath).isDefined

  val isSpring: ArtifactSource => Boolean = project =>
    GetMavenPom(project).exists(f => f.content.contains("org.springframework"))

  val SpringBootStarterParent: String = "spring-boot-starter-parent"

  val isSpringBoot: ArtifactSource => Boolean = project =>
    isSpring(project) &&
      GetMavenPom(project).exists(f => f.content.contains(SpringBootStarterParent))
}