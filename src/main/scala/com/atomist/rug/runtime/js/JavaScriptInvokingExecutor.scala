package com.atomist.rug.runtime.js

import com.atomist.project.{Executor, ProjectOperationArguments}
import com.atomist.rug.kind.service.{ServiceSource, ServicesMutableView, ServicesType}
import com.atomist.rug.runtime.js.interop.jsSafeCommittingProxy
import com.atomist.source.ArtifactSource
import jdk.nashorn.api.scripting.ScriptObjectMirror

class JavaScriptInvokingExecutor(
                                  jsc: JavaScriptContext,
                                  jsVar: ScriptObjectMirror,
                                  rugAs: ArtifactSource
                                )
  extends JavaScriptInvokingProjectOperation(jsc, jsVar, rugAs)
    with Executor {

  override val name: String = jsVar.getMember("name").asInstanceOf[String]

  override def execute(serviceSource: ServiceSource, poa: ProjectOperationArguments): Unit = {
    val smv = new ServicesMutableView(rugAs, serviceSource)
    val wsmv = new jsSafeCommittingProxy(new ServicesType, smv)
    //val reviewContext = new ReviewContext
    invokeMemberWithParameters("execute", wsmv, poa)
  }
}
