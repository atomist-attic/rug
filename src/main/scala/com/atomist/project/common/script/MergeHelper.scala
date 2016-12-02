package com.atomist.project.common.script

import com.atomist.project.common.template.{MergeContext, MergeTool}

class MergeHelper(mt: MergeTool) {

  def merge(m: Map[String, Object], templateName: String): String = {
    mt.mergeToFile(MergeContext(m), templateName).content
  }
}
