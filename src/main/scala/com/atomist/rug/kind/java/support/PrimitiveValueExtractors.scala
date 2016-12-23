package com.atomist.rug.kind.java.support

import com.atomist.source.{ArtifactSource, FileArtifact}
import com.atomist.util.lang.{JavaHelpers, MavenConstants}

import scala.collection.JavaConverters._

case class SourcePaths(
                        baseSourcePath: String,
                        baseTestPath: String
                      )

import com.atomist.util.lang.JavaConstants._

object DefaultSourcePaths extends SourcePaths(
  DefaultBaseSourcePath,
  DefaultBaseTestPath
)

object GetMavenPom  {

  def apply(as: ArtifactSource): Option[FileArtifact] =
    as.findFile(MavenConstants.PomPath)
}

class UnderPathExtractor(path: String = "") {

  def apply(as: ArtifactSource): ArtifactSource = as / path
}

/**
  * Preserve existing paths in entire artifact.
  */
object JavaFilesExtractor {

  def apply(project: ArtifactSource) = {
    project.allFiles.filter(JavaHelpers.isJavaSourceArtifact(_)).asJava
  }
}
