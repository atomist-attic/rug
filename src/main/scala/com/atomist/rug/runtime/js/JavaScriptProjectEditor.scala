package com.atomist.rug.runtime.js

import com.atomist.param.ParameterValues
import com.atomist.project.archive.{DefaultAtomistConfig, RugResolver}
import com.atomist.project.edit._
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.source.ArtifactSource
import com.atomist.util.Timing._
import jdk.nashorn.api.scripting.ScriptObjectMirror

import scala.util.control.NonFatal

/**
  * Find Editors in a Nashorn.
  */
class JavaScriptProjectEditorFinder
  extends JavaScriptRugFinder[JavaScriptProjectEditor] {

  /**
    * Is the supplied thing valid at all?
    */
  def isValid(obj: ScriptObjectMirror): Boolean =
    obj.getMember("__kind") == "editor" &&
      obj.hasMember("edit") &&
      obj.getMember("edit").asInstanceOf[ScriptObjectMirror].isFunction

  override def create(jsc: JavaScriptContext, fnVar: ScriptObjectMirror, resolver: Option[RugResolver]): Option[JavaScriptProjectEditor] = {
    Some(new JavaScriptProjectEditor(jsc, fnVar, jsc.rugAs, resolver))
  }
}

/**
  * ProjectEditor implementation that invokes a JavaScript function.
  */
class JavaScriptProjectEditor(jsc: JavaScriptContext,
                              jsVar: ScriptObjectMirror,
                              rugAs: ArtifactSource,
                              resolver: Option[RugResolver])
  extends JavaScriptProjectOperation(jsc, jsVar, rugAs)
    with ProjectEditorSupport {

  override def applicability(as: ArtifactSource): Applicability = Applicability.OK

  override protected def modifyInternal(targetProject: ArtifactSource, poa: ParameterValues): ModificationAttempt = {
    val validated = addDefaultParameterValues(poa)
    validateParameters(validated)

    val (result, elapsedTime) = time {
      val pmv = new ProjectMutableView(rugAs, targetProject, atomistConfig = DefaultAtomistConfig, Some(this), rugResolver = resolver)
      try {
        // Important that we don't invoke edit on the prototype as otherwise all constructor effects are lost!
        invokeMemberFunction(jsc, jsVar, "edit", Some(validated), wrapProject(pmv))
        if (pmv.currentBackingObject == targetProject) {
          NoModificationNeeded("OK")
        } else {
          SuccessfulModification(pmv.currentBackingObject, pmv.changeLogEntries)
        }
      } catch {
        case sle: SourceLanguageRuntimeException =>
          throw sle
        case NonFatal(t) =>
          throw new RuntimeException(s"Editor '$name' failed due to ${t.getMessage}", t)
      }
    }
    logger.debug(s"$name modifyInternal took ${elapsedTime}ms")
    result
  }
}
