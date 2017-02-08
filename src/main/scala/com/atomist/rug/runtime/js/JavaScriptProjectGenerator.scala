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
class JavaScriptProjectGenerator(
                                          jsc: JavaScriptContext,
                                          jsVar: ScriptObjectMirror,
                                          rugAs: ArtifactSource,
                                          startProject: ArtifactSource
                                        )
  extends JavaScriptProjectOperation(jsc, jsVar, rugAs)
    with ProjectGenerator {

  @throws(classOf[InvalidParametersException])
  override def generate(projectName: String, poa: ProjectOperationArguments): ArtifactSource = {
    validateParameters(poa)
    val tr = time {
      val project = new EmptyArtifactSource(projectName) + startProject
      val pmv = new ProjectMutableView(rugAs, project, atomistConfig = DefaultAtomistConfig, context)
      invokeMemberFunction(jsc, jsVar, "populate", wrapProject(pmv), poa)
      pmv.currentBackingObject
    }
    logger.debug(s"$name modifyInternal took ${tr._2}ms")
    tr._1
  }

}
