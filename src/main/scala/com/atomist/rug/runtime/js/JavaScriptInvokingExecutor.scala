package com.atomist.rug.runtime.js

import com.atomist.project.{Executor, ProjectOperationArguments}
import com.atomist.rug.kind.service.{ServiceSource, ServicesMutableView}
import com.atomist.source.ArtifactSource
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
    val smv = new ServicesMutableView(rugAs, serviceSource)
    //val reviewContext = new ReviewContext
    invokeMemberWithParameters("execute", smv, parameters)
  }
}
