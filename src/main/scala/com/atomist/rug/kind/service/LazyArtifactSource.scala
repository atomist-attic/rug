package com.atomist.rug.kind.service

import com.atomist.source._

/**
  * Lazy ArtifactSource to pass into ProjectMutableViews.
  * We don't read the actual content unless we need to.
  * Not threadsafe.
  * @param loc locator that enables us to load this ArtifactSource
  * @param materialize
  */
class LazyArtifactSource(
                          loc: ArtifactSourceLocator,
                          materialize: () => ArtifactSource)
  extends ArtifactSource {

  private var underlying: ArtifactSource = _

  private def delegate(): ArtifactSource = {
    if (underlying == null)
      underlying = materialize()
    underlying
  }

  override lazy val id: ArtifactSourceIdentifier = loc match {
    case i: ArtifactSourceIdentifier => i
    case x => delegate().id
  }

  override def allDirectories: Seq[DirectoryArtifact] = delegate().allDirectories

  override def allFiles: Seq[FileArtifact] = delegate().allFiles

  override def artifacts: Seq[Artifact] = delegate().artifacts

}
