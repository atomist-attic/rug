package com.atomist.rug.runtime.js

import com.atomist.project.ProjectOperationArguments
import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.project.edit.{ProjectEditorSupport, _}
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.spi.InstantEditorFailureException
import com.atomist.source.ArtifactSource
import com.atomist.util.Timing._
import jdk.nashorn.api.scripting.ScriptObjectMirror

/**
  * ProjectEditor implementation that invokes a JavaScript function. This will probably be the result of
  * TypeScript compilation, but need not be. Attempts to source metadata from annotations.
  */
class JavaScriptInvokingProjectEditor(
                                   jsc: JavaScriptContext,
                                   jsVar: ScriptObjectMirror,
                                   rugAs: ArtifactSource
                                 )
  extends JavaScriptInvokingProjectOperation(jsc, jsVar, rugAs)
    with ProjectEditorSupport {

  override val name: String = jsVar.getMember("name").asInstanceOf[String]

  override def impacts: Set[Impact] = Impacts.UnknownImpacts

  override def applicability(as: ArtifactSource): Applicability = Applicability.OK

  override protected def modifyInternal(
                                         targetProject: ArtifactSource,
                                         poa: ProjectOperationArguments): ModificationAttempt = {
    val (result, elapsedTime) = time {
      val pmv = new ProjectMutableView(rugAs,
        targetProject,
        atomistConfig = DefaultAtomistConfig,
        context)

      try {
        //important that we don't invoke edit on the prototype as otherwise all constructor effects are lost!
        val res = invokeMemberWithParameters("edit",
          wrapProject(pmv),
          poa)

        if (pmv.currentBackingObject == targetProject) {
          NoModificationNeeded("OK")
        }
        else {
          SuccessfulModification(pmv.currentBackingObject, impacts, "OK")
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