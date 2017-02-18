package com.atomist.rug.runtime.js

import com.atomist.param.ParameterValues
import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.project.edit.{ProjectEditorSupport, _}
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.spi.InstantEditorFailureException
import com.atomist.source.ArtifactSource
import com.atomist.util.Timing._
import jdk.nashorn.api.scripting.ScriptObjectMirror

/**
  * Find Editors in a Nashorn
  */
class JavaScriptProjectEditorFinder
  extends JavaScriptProjectOperationFinder[JavaScriptProjectEditor]{

  override def signatures: Set[JsRugOperationSignature] = Set(
    JsRugOperationSignature(Set("edit"),Set("name", "description")),
    JsRugOperationSignature(Set("edit"), Set("__name", "__description")))

  override def createProjectOperation(jsc: JavaScriptContext, fnVar: ScriptObjectMirror): JavaScriptProjectEditor = {
    new JavaScriptProjectEditor(jsc, fnVar, jsc.rugAs)
  }
}
/**
  * ProjectEditor implementation that invokes a JavaScript function.
  */
class JavaScriptProjectEditor(
                                       jsc: JavaScriptContext,
                                       jsVar: ScriptObjectMirror,
                                       rugAs: ArtifactSource
                                     )
  extends JavaScriptProjectOperation(jsc, jsVar, rugAs)
    with ProjectEditorSupport {

  override def applicability(as: ArtifactSource): Applicability = Applicability.OK

  override protected def modifyInternal(
                                         targetProject: ArtifactSource,
                                         poa: ParameterValues): ModificationAttempt = {
    val (result, elapsedTime) = time {
      val pmv = new ProjectMutableView(rugAs,
        targetProject,
        atomistConfig = DefaultAtomistConfig,
        context)

      try {
        //important that we don't invoke edit on the prototype as otherwise all constructor effects are lost!
        invokeMemberFunction(
          jsc,
          jsVar,
          "edit",
          wrapProject(pmv),
          poa)

        if (pmv.currentBackingObject == targetProject) {
          NoModificationNeeded("OK")
        }
        else {
          SuccessfulModification(pmv.currentBackingObject, pmv.changeLogEntries)
        }
      }
      catch {
        case f: InstantEditorFailureException =>
          FailedModificationAttempt(f.getMessage)
      }
    }
    logger.debug(s"$name modifyInternal took ${elapsedTime}ms")
    result
  }
}