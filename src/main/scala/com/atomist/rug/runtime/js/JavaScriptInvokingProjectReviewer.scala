package com.atomist.rug.runtime.js

import com.atomist.project.ProjectOperationArguments
import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.project.review.{ProjectReviewer, ReviewResult}
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.source.ArtifactSource
import com.atomist.util.Timing._
import jdk.nashorn.api.scripting.ScriptObjectMirror

import scala.collection.JavaConverters._

class JavaScriptInvokingProjectReviewer(
                                         jsc: JavaScriptContext,
                                         jsVar: ScriptObjectMirror,
                                         rugAs: ArtifactSource
                                       )
  extends JavaScriptInvokingProjectOperation(jsc, jsVar, rugAs)
    with ProjectReviewer {

  override val name: String = jsVar.getMember("name").asInstanceOf[String]

  override def review(targetProject: ArtifactSource, poa: ProjectOperationArguments): ReviewResult = {
    val (response, elapsedTime) = time {
      val pmv = new ProjectMutableView(rugAs,
        targetProject,
        atomistConfig = DefaultAtomistConfig,
        context)

      invokeMemberWithParameters("review",
        wrapProject(pmv),
        poa) match {
        case m: ScriptObjectMirror =>
          m
      }
    }
    logger.debug(s"$name review took ${elapsedTime}ms")
    convertJavaScriptResponsToReviewResult(response)
  }

  private def convertJavaScriptResponsToReviewResult(response: ScriptObjectMirror): ReviewResult = {

    val responseNote = response.get("note")
    val responseComments = response.get("comments") match {
      case som: ScriptObjectMirror => som
    }

    ReviewResult(responseNote.toString)
  }
}
