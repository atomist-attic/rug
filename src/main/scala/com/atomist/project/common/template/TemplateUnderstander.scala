package com.atomist.project.common.template

import com.atomist.project.ProjectOperation
import com.atomist.source.ArtifactSource

/**
  * Recognizes a template.
  */
trait TemplateUnderstander[PO <: ProjectOperation] {

  /**
    * Create a new ProjectOperation if we understand this template.
    */
  def create(metaContent: ArtifactSource,
             startingPoint: ArtifactSource,
             templateContent: ArtifactSource): Option[PO]
}
