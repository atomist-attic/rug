package com.atomist.rug.runtime

import com.atomist.project.ProjectOperationArguments
import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.project.edit.{ProjectEditorSupport, _}
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.spi.{InstantEditorFailureException, TypeRegistry}
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

  val typeRegistry: TypeRegistry = DefaultTypeRegistry
  val projectType = typeRegistry.findByName("project").getOrElse(throw new TypeNotPresentException("project", null))

  override val name: String =
    if (className.endsWith("Editor")) className.dropRight("Editor".length)
    else className

  override def impacts: Set[Impact] = Impacts.UnknownImpacts

  override def applicability(as: ArtifactSource): Applicability = Applicability.OK

  override protected def modifyInternal(
                                         targetProject: ArtifactSource,
                                         poa: ProjectOperationArguments): ModificationAttempt = {
    val tr = time {
      val pmv = new ProjectMutableView(rugAs, targetProject, atomistConfig = DefaultAtomistConfig, context)

      try {
        //important that we don't invoke edit on the prototype as otherwise all constructor effects are lost!
        val res = invokeMemberWithParameters("edit",
          pmv,
          //wrapperize(pmv),
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
    logger.debug(s"$name modifyInternal took ${tr._2}ms")
    tr._1
  }

  private def wrapperize(pmv: ProjectMutableView): SafeCommittingProxy = {
    new SafeCommittingProxy(projectType ,pmv)
  }
}