package com.atomist.rug.runtime

import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.project.common.InvalidParametersException
import com.atomist.project.{Executor, ProjectOperationArguments}
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.kind.service.ServiceSource
import com.atomist.source.{ArtifactSource, EmptyArtifactSource}
import jdk.nashorn.api.scripting.ScriptObjectMirror

class JavaScriptInvokingExecutor(
                                  jsc: JavaScriptContext,
                                  className: String,
                                  jsVar: ScriptObjectMirror,
                                  rugAs: ArtifactSource
                                )
  extends JavaScriptInvokingProjectOperation(jsc, className, jsVar, rugAs)
    with Executor {

  override val name: String =
    if (className.endsWith("Executor")) className.dropRight("Executor".length)
    else className


  override def execute(serviceSource: ServiceSource, poa: ProjectOperationArguments): Unit = {
    //val result = jsc.engine.get(className.toLowerCase).asInstanceOf[ScriptObjectMirror].callMember("populate", pmv, params)
???
  }


}
