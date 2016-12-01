package com.atomist.project.common.template

import _root_.java.io.InputStream

import com.atomist.source.ArtifactSource
import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.collections.ExtendedProperties
import org.apache.velocity.exception.ResourceNotFoundException
import org.apache.velocity.runtime.resource.Resource
import org.apache.velocity.runtime.resource.loader.ResourceLoader

/**
  * Velocity ResourceLoader backed by an ArtifactSource.
  */
class ArtifactSourceBackedVelocityResourceLoader(artifactSource: ArtifactSource)
  extends ResourceLoader
    with LazyLogging {

  override def init(configuration: ExtendedProperties): Unit = {
  }

  override def isSourceModified(resource: Resource): Boolean = false

  override def getResourceStream(source: String): InputStream = {
    logger.debug(s"Need to return ResourceStream for $source")
    artifactSource.findFile(source).getOrElse {
      throw new ResourceNotFoundException(s"Didn't find '$source' in artifactSource")
    }.inputStream
  }

  override def getLastModified(resource: Resource): Long = 0
}
