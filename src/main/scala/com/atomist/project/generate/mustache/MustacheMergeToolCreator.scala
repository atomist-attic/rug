package com.atomist.project.generate.mustache

import com.atomist.project.common.template.MergeToolCreator
import com.atomist.source.ArtifactSource
import com.atomist.util.template.MergeTool
import com.atomist.util.template.mustache.MustacheMergeTool

class MustacheMergeToolCreator
  extends MergeToolCreator {

  override def createMergeTool(templateContent: ArtifactSource): MergeTool =
    new MustacheMergeTool(templateContent)
}
