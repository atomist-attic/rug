package com.atomist.util.misc

import com.atomist.source.ArtifactSource
import com.atomist.source.file.ClassPathArtifactSource

/**
  * Helps loading resources from a package
  */
object ResourceLoading {

  /**
    * Return all resources in this package as an ArtifactSource.
    * All resources will be in the root
    */
  def resourcesInPackage(resourcePath: String): ArtifactSource = {
    val as = ClassPathArtifactSource.toArtifactSource(resourcePath)
        .filter(_ => true, f => !f.name.endsWith(".class"))
    if (as.empty) {
      throw new IllegalArgumentException(s"Can't load resources at class path resource [$resourcePath]")
    }
    as
  }

  def classToPath(caller: Object): String =
    caller.getClass.getPackage.getName.replace(".", "/")

}
