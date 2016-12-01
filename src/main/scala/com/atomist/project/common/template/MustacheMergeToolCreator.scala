package com.atomist.project.common.template

import com.atomist.source.ArtifactSource

class MustacheMergeToolCreator
  extends MergeToolCreator {

  override def createMergeTool(templateContent: ArtifactSource): MergeTool =
    new MustacheMergeTool(templateContent)
}
