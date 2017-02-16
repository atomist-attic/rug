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
  val KnownSignatures = Set(
    JsRugOperationSignature(ExecutorType, Set("execute")),
    JsRugOperationSignature(ExecutorType, Set("execute"), Set("__name", "__description")),
    JsRugOperationSignature(EditorType, Set("edit")),
    JsRugOperationSignature(EditorType, Set("edit"), Set("__name", "__description")),
    JsRugOperationSignature(ReviewerType,Set("review")),
    JsRugOperationSignature(ReviewerType,Set("review"), Set("__name", "__description")),
    JsRugOperationSignature(GeneratorType,Set("populate")),
    JsRugOperationSignature(GeneratorType,Set("populate"),Set("__name", "__description"))
  )

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
    jsc.vars.map(o => extractOperation(o.scriptObjectMirror)) collect {
      case Some((EditorType, v)) =>
        new JavaScriptInvokingProjectEditor(jsc, v, rugAs)
      case Some((ReviewerType, v)) =>
        new JavaScriptInvokingProjectReviewer(jsc, v, rugAs)
      case Some((GeneratorType, v)) =>
        // TODO properly fix the following
        import com.atomist.project.archive.ProjectOperationArchiveReaderUtils.removeAtomistTemplateContent
        val project: ArtifactSource = removeAtomistTemplateContent(rugAs)
        new JavaScriptInvokingProjectGenerator(jsc, v, rugAs, project)
      case Some((ExecutorType, v)) =>
        new JavaScriptInvokingExecutor(jsc, v, rugAs)
    }
  }

  private def extractOperation(exported: ScriptObjectMirror): Option[(String, ScriptObjectMirror)] = {
    val obj = exported.getMember("prototype") match {
      case modern: ScriptObjectMirror => modern
      case _ => exported
    }
    KnownSignatures.find {
      case JsRugOperationSignature(_, fns, props) =>
        val fnCount = fns.count(fn => {
          obj.hasMember(fn) && obj.getMember(fn).asInstanceOf[ScriptObjectMirror].isFunction
        })
        val propsCount = props.count(prop => {
          obj.hasMember(prop) // TODO make stronger check
        })
        fnCount == fns.size && propsCount == props.size
    } match {
      case Some(JsRugOperationSignature(kind,_, _)) => Some(kind, obj)
      case _ => Option.empty
    }
  }

  case class JsRugOperationSignature(rugtype: String, functionsNames: Set[String], propertyNames: Set[String] = Set("name", "description"))
}
