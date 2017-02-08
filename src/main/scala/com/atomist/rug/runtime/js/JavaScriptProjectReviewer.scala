package com.atomist.rug.runtime.js

import com.atomist.project.ProjectOperationArguments
import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.project.review.{ProjectReviewer, ReviewComment, ReviewResult, Severity}
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.source.ArtifactSource
import com.atomist.util.Timing._
import jdk.nashorn.api.scripting.ScriptObjectMirror

import scala.collection.JavaConverters._

class JavaScriptProjectReviewer(
                                         jsc: JavaScriptContext,
                                         jsVar: ScriptObjectMirror,
                                         rugAs: ArtifactSource
                                       )
  extends JavaScriptProjectOperation(jsc, jsVar, rugAs)
    with ProjectReviewer {

  override def review(targetProject: ArtifactSource, poa: ProjectOperationArguments): ReviewResult = {
    val (response, elapsedTime) = time {
      val pmv = new ProjectMutableView(rugAs,
        targetProject,
        atomistConfig = DefaultAtomistConfig,
        context)

      invokeMemberFunction(
        jsc,
        jsVar,
        "review",
        wrapProject(pmv),
        addDefaultParameterValues(poa)) match {
        case m: ScriptObjectMirror =>
          m
      }
    }
    logger.debug(s"$name review took ${elapsedTime}ms")
    convertJavaScriptResponseToReviewResult(response)
  }

  private def convertJavaScriptResponseToReviewResult(response: ScriptObjectMirror): ReviewResult = {

    val responseNote = response.get("note")
    val reviewResult: ReviewResult = response.get("comments") match {
      case som: ScriptObjectMirror =>
        if (som.isArray) {
          val convertedComments:Iterable[ReviewComment] = som.asScala.values.map {
            case commentSom: ScriptObjectMirror =>
              ReviewComment(
                commentSom.get("comment").toString,
                Severity(commentSom.get("severity").toString.toInt),
                commentSom.get("fileName") match {
                  case null => None
                  case fileName: String => Option(fileName)
                },
                commentSom.get("line") match {
                  case null => None
                  case line => Option(line.toString.toInt)
                },
                commentSom.get("column") match {
                  case null => None
                  case column => Option(column.toString.toInt)
                }
              )
          }
          ReviewResult(responseNote.toString, convertedComments.to)
        } else {
          ReviewResult(responseNote.toString)
        }
    }

    reviewResult
  }
}
