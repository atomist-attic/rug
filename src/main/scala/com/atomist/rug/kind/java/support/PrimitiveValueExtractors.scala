package com.atomist.rug.kind.java.support

import com.atomist.project.{ArtifactSourceFilter, FilesExtractor, MaybeFileExtractor, ProjectValueExtractor}
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

object SourcePathExtractor extends ProjectValueExtractor[SourcePaths] {

  override def apply(project: ArtifactSource): SourcePaths = {
    DefaultSourcePaths
  }
}

object GetMavenPom extends MaybeFileExtractor {

  override def apply(as: ArtifactSource): Option[FileArtifact] =
    as.findFile(MavenConstants.PomPath)
}

class UnderPathExtractor(path: String = "") extends ArtifactSourceFilter {

  override def apply(as: ArtifactSource): ArtifactSource = as / path
}

/**
  * Extract Java files under base path.
  */
object JavaBaseTreeExtractor extends ArtifactSourceFilter {

  override def apply(project: ArtifactSource): ArtifactSource = {
    val sourcePaths = SourcePathExtractor.apply(project)
    val javaSource: ArtifactSource = project / sourcePaths.baseSourcePath
    javaSource
  }
}

/**
  * Preserve existing paths in entire artifact.
  */
object JavaFilesExtractor extends FilesExtractor {

  override def apply(project: ArtifactSource) = {
    project.allFiles.filter(JavaHelpers.isJavaSourceArtifact(_)).asJava
  }
}
