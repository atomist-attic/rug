package com.atomist.rug.runtime

import com.atomist.project.ProjectOperationArguments
import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.project.edit._
import com.atomist.project.edit.common.ProjectEditorSupport
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
                                   className: String,
                                   jsVar: ScriptObjectMirror,
                                   rugAs: ArtifactSource
                                 )
  extends JavaScriptInvokingProjectOperation(jsc, className, jsVar, rugAs)
    with ProjectEditorSupport {

  override val name: String =
    if (className.endsWith("Editor")) className.dropRight("Editor".length)
    else className

  override def impacts: Set[Impact] = Impacts.UnknownImpacts

  override def applicability(as: ArtifactSource): Applicability = Applicability.OK

  override protected def modifyInternal(
                                         targetProject: ArtifactSource,
                                         poa: ProjectOperationArguments): ModificationAttempt = {
    val tr = time {
      val pmv = new ProjectMutableView(rugAs, targetProject, atomistConfig = DefaultAtomistConfig)

      val params = new BidirectionalParametersProxy(poa)

      //  println(editMethod.entrySet())

      try {
        //important that we don't invoke edit on the prototype as otherwise all constructor effects are lost!
        val res = jsc.engine.get(className.toLowerCase).asInstanceOf[ScriptObjectMirror].callMember("edit", pmv, params)

        println("Params=" + params)

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
    logger.debug(s"$name modifyInternal took ${tr._2}ms")
    tr._1
  }

}

