package com.atomist.project.common.template

import java.io.{Reader, StringReader}

import com.atomist.source.ArtifactSource
import com.github.mustachejava.resolver.DefaultResolver
import com.typesafe.scalalogging.LazyLogging

class ArtifactSourceBackedMustacheResolver(artifactSource: ArtifactSource)
  extends DefaultResolver
    with LazyLogging{

  override def getReader(resourceName: String): Reader = {
    logger.debug(s"Need to return Reader for $resourceName")
    artifactSource.findFile(resourceName) match {
      case Some(f) => new StringReader(f.content)
      case _ => new StringReader(resourceName)
    }
  }
}
