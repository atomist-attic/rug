package com.atomist.util.template

import com.atomist.source.FileArtifact

/**
  * Combines template handling from different merge tools
  *
  * @param mergeTools
  */
class CombinedMergeTool(
                         mergeTools: Seq[MergeTool]
                       ) extends MergeTool {

  override def isTemplate(templateName: String): Boolean =
    mergeTools.exists(mt => mt.isTemplate(templateName))

  override def mergeString(context: MergeContext, templateString: String): String = {
    mergeTools.head.mergeString(context, templateString)
  }

  override def mergeToFile(context: MergeContext, templateName: String): FileArtifact =
    mergeTools.find(mt => mt.isTemplate(templateName))
      .map(mt => mt.mergeToFile(context, templateName))
      .getOrElse(
        throw new IllegalArgumentException(s"Unable to find a MergeTool for supposed template '$templateName'")
      )

}
