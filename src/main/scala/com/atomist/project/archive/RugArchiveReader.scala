package com.atomist.project.archive

import com.atomist.rug.runtime.Rug
import com.atomist.source.ArtifactSource

/**
  * Find rugs in Artifact Sources
  */
trait RugArchiveReader[T <: Rug] {
  /**
    *
    * @param as
    * @param namespace
    * @param otherRugs - other rugs brought in via manifest deps
    * @return
    */
  def find(as: ArtifactSource, namespace: Option[String], otherRugs: Seq[Rug]) : Rugs
}
