package com.atomist.project.generate.velocity

import com.atomist.project.common.template.MergeToolCreator
import com.atomist.source.ArtifactSource
import com.atomist.util.template.MergeTool
import com.atomist.util.template.velocity.VelocityMergeTool

class VelocityMergeToolCreator
  extends MergeToolCreator {

  override def createMergeTool(templateContent: ArtifactSource): MergeTool =
    new VelocityMergeTool(templateContent)
}
