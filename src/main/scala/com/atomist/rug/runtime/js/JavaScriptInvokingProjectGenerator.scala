package com.atomist.rug.runtime.js

import com.atomist.project.ProjectOperationArguments
import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.project.common.InvalidParametersException
import com.atomist.project.generate.ProjectGenerator
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.source.{ArtifactSource, EmptyArtifactSource}
import com.atomist.util.Timing._

import jdk.nashorn.api.scripting.ScriptObjectMirror

/**
  * ProjectEditor implementation that invokes a JavaScript function. This will probably be the result of
  * TypeScript compilation, but need not be. Attempts to source metadata from annotations.
  */
class JavaScriptInvokingProjectGenerator(
                                          jsc: JavaScriptContext,
                                          jsVar: ScriptObjectMirror,
                                          rugAs: ArtifactSource,
                                          startProject: ArtifactSource
                                        )
  extends JavaScriptInvokingProjectOperation(jsc, jsVar, rugAs)
    with ProjectGenerator {

  override val name: String = jsVar.getMember("name").asInstanceOf[String]

  @throws(classOf[InvalidParametersException])
  override def generate(poa: ProjectOperationArguments): ArtifactSource = {
    validateParameters(poa)
    val tr = time {
      val pmv = new ProjectMutableView(rugAs, startProject, atomistConfig = DefaultAtomistConfig, context)
      invokeMemberWithParameters("populate", wrapProject(pmv), poa.parameterValueMap("project_name").getValue, poa)
      pmv.currentBackingObject
    }
    logger.debug(s"$name modifyInternal took ${tr._2}ms")
    tr._1
  }

}
