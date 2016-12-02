package com.atomist.rug.kind.java.support

import com.atomist.project.{FunctionProjectAssertion, ProjectAssertion}
import com.atomist.source.ArtifactSource
import com.atomist.util.lang.{JavaHelpers, MavenConstants}

import scala.collection.JavaConversions._

object IsJavaProject extends FunctionProjectAssertion(project =>
  project.allFiles.exists(f => JavaHelpers.isJavaSourceArtifact(f))
)

object IsMavenProject extends FunctionProjectAssertion(project =>
  project.findFile(MavenConstants.PomPath).isDefined
)

/**
  * Is this a Spring Project?
  * TODO currently supports only Spring Maven projects
  */
object IsSpringProject extends ProjectAssertion {

  override def apply(project: ArtifactSource): Boolean =
    GetMavenPom(project).exists(f => f.content.contains("org.springframework"))
}

/**
  * Is this a Spring Boot Project
  */
object IsSpringBootProject extends ProjectAssertion {

  val SpringBootStarterParent = "spring-boot-starter-parent"

  override def apply(project: ArtifactSource) =
    IsSpringProject(project) &&
      GetMavenPom(project).exists(f => f.content.contains(SpringBootStarterParent))
}