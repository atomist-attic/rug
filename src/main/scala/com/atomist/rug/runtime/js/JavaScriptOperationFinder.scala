package com.atomist.rug.runtime.js

import com.atomist.project.ProjectOperation
import com.atomist.source.ArtifactSource
import jdk.nashorn.api.scripting.ScriptObjectMirror

/**
  * Find and instantiate JavaScript editors in a Rug archive
  */
object JavaScriptOperationFinder {

  val ExecutorType = "executor"

  val EditorType = "editor"

  val ReviewerType = "reviewer"

  val GeneratorType = "generator"

  /**
    * Used to recognise JS operations that we can call.
    * TODO - this should probably include type checking too!
    */
  val KnownSignatures = Map(
    ExecutorType -> JsRugOperationSignature(Set("execute")),
    EditorType -> JsRugOperationSignature(Set("edit")),
    ReviewerType -> JsRugOperationSignature(Set("review")),
    GeneratorType -> JsRugOperationSignature(Set("populate")))

  /**
    * Find and instantiate project operations in the given Rug archive
    *
    * @param rugAs archive to look into
    * @return a sequence of instantiated operations backed by JavaScript
    */
  def fromJavaScriptArchive(rugAs: ArtifactSource): Seq[ProjectOperation] = {
    operationsFromVars(rugAs, new JavaScriptContext(rugAs))
  }

  // TODO clean up this dispatch/signature stuff - too coupled
  private def operationsFromVars(rugAs: ArtifactSource, jsc: JavaScriptContext): Seq[JavaScriptInvokingProjectOperation] = {
    jsc.vars.map(v => (v, extractOperation(v.scriptObjectMirror))) collect {
      case (v, Some(EditorType)) =>
        new JavaScriptInvokingProjectEditor(jsc, v.scriptObjectMirror, rugAs)
      case (v, Some(ReviewerType)) =>
        new JavaScriptInvokingProjectReviewer(jsc, v.scriptObjectMirror, rugAs)
      case (v, Some(GeneratorType)) =>
        // TODO properly fix the following
        import com.atomist.project.archive.ProjectOperationArchiveReaderUtils.removeAtomistTemplateContent
        val project: ArtifactSource = removeAtomistTemplateContent(rugAs)
        new JavaScriptInvokingProjectGenerator(jsc, v.scriptObjectMirror, rugAs, project)
      case (v, Some(ExecutorType)) =>
        new JavaScriptInvokingExecutor(jsc, v.scriptObjectMirror, rugAs)
    }
  }

  private def extractOperation(obj: ScriptObjectMirror): Option[String] = {
    val matches = KnownSignatures.foldLeft(Seq[String]())((acc: Seq[String], kv) => {
      //does it contain all the matching functions and props?
      val fns = kv._2.functionsNames
      val props = kv._2.propertyNames
      val fnCount = fns.count(fn => {
        obj.hasMember(fn) && obj.getMember(fn).asInstanceOf[ScriptObjectMirror].isFunction
      })
      val propsCount = props.count(prop => {
        obj.hasMember(prop) // TODO make stronger check
      })
      if (fnCount == fns.size && propsCount == props.size) {
        acc :+ kv._1
      } else {
        acc
      }
    })
    matches.headOption
  }

  case class JsRugOperationSignature(functionsNames: Set[String], propertyNames: Set[String] = Set("name", "description"))
}
