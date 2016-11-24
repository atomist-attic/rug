package com.atomist.project.common.template

import com.atomist.source.ArtifactSource
import com.atomist.util.template.{CombinedMergeTool, MergeTool}

trait MergeToolCreator {

  def createMergeTool(templateContent: ArtifactSource): MergeTool
}

class CombinedMergeToolCreator(
                                creators: MergeToolCreator*
                              ) extends MergeToolCreator {

  override def createMergeTool(templateContent: ArtifactSource): MergeTool = {
    new CombinedMergeTool(creators.map(c => c.createMergeTool(templateContent)))
  }
}