package com.atomist.rug.runtime.js

import com.atomist.param.ParameterValues
import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.project.review.{ProjectReviewer, ReviewComment, ReviewResult, Severity}
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.runtime.js.interop.NashornUtils
import com.atomist.source.ArtifactSource
import com.atomist.util.Timing._
import jdk.nashorn.api.scripting.ScriptObjectMirror

import scala.collection.JavaConverters._

/**
  * Find Reviewers in a Rug archive processed using Nashorn
  */
class JavaScriptProjectReviewerFinder
  extends JavaScriptProjectOperationFinder[JavaScriptProjectReviewer] {

  override def signatures: Set[JsRugOperationSignature] = Set(
    JsRugOperationSignature(Set("review"),Set("name", "description")),
    JsRugOperationSignature(Set("review"), Set("__name", "__description")))

  override def createProjectOperation(jsc: JavaScriptContext, fnVar: ScriptObjectMirror): JavaScriptProjectReviewer = {
    new JavaScriptProjectReviewer(jsc, fnVar, jsc.rugAs)
  }
}

/**
  * A project reviewer.
  * @param rugAs backing artifact source for the Rug archive
  */
class JavaScriptProjectReviewer(
                                 jsc: JavaScriptContext,
                                 jsVar: ScriptObjectMirror,
                                 rugAs: ArtifactSource
                               )
  extends JavaScriptProjectOperation(jsc, jsVar, rugAs)
    with ProjectReviewer {

  override def review(targetProject: ArtifactSource, poa: ParameterValues): ReviewResult = {
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

    val responseNote = NashornUtils.stringProperty(response, "note", "")
    val reviewResult: ReviewResult = response.get("comments") match {
      case som: ScriptObjectMirror =>
        if (som.isArray) {
          val convertedComments:Iterable[ReviewComment] = som.asScala.values.map {
            case commentSom: ScriptObjectMirror =>
              val severity = Severity(NashornUtils.stringProperty(commentSom, "severity", "" + Severity.MAJOR.id).toInt)
              ReviewComment(
                NashornUtils.stringProperty(commentSom, "comment", ""),
                severity,
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
          ReviewResult(responseNote, convertedComments.to)
        }
        else {
          ReviewResult(responseNote)
        }
    }

    reviewResult
  }
}
