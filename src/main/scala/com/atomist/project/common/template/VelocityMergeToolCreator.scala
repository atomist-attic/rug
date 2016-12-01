package com.atomist.project.common.template

import com.atomist.source.ArtifactSource

class VelocityMergeToolCreator
  extends MergeToolCreator {

  override def createMergeTool(templateContent: ArtifactSource): MergeTool =
    new VelocityMergeTool(templateContent)
}
