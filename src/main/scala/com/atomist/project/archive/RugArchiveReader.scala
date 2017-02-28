package com.atomist.project.archive

import com.atomist.rug.runtime.AddressableRug
import com.atomist.source.ArtifactSource

/**
  * Find rugs in Artifact Sources
  */
trait RugArchiveReader {
  /**
    *
    * @param as
    * @param otherRugs - other rugs brought in via manifest deps
    * @return
    */
  def find(as: ArtifactSource, otherRugs: Seq[AddressableRug]) : Rugs
}
