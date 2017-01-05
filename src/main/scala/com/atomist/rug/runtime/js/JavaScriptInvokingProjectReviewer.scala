package com.atomist.rug.runtime.js

import com.atomist.project.ProjectOperationArguments
import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.project.review.{ProjectReviewer, ReviewResult}
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.source.ArtifactSource
import com.atomist.util.Timing._
import jdk.nashorn.api.scripting.ScriptObjectMirror

class JavaScriptInvokingProjectReviewer(
                                         jsc: JavaScriptContext,
                                         jsVar: ScriptObjectMirror,
                                         rugAs: ArtifactSource
                                       )
  extends JavaScriptInvokingProjectOperation(jsc, jsVar, rugAs)
    with ProjectReviewer {

  override val name: String = jsVar.getMember("name").asInstanceOf[String]

  override def review(targetProject: ArtifactSource, poa: ProjectOperationArguments): ReviewResult = {
    val (result, elapsedTime) = time {
      val pmv = new ProjectMutableView(rugAs,
        targetProject,
        atomistConfig = DefaultAtomistConfig,
        context)

      try {
        val res = invokeMemberWithParameters("review",
          wrapProject(pmv),
          poa)
      }
    }
    logger.debug(s"$name review took ${elapsedTime}ms")
    null
  }
}
