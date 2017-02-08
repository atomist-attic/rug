package com.atomist.rug.runtime.js

import com.atomist.param.{Parameter, ParameterValues, Tag}
import com.atomist.rug.runtime.js.interop.JavaScriptHandlerContext
import com.atomist.rug.runtime.{InstructionResponse, ParameterizedHandler, ResponseHandler}
import com.atomist.rug.spi.Plan.Plan
import com.atomist.source.ArtifactSource
import jdk.nashorn.api.scripting.ScriptObjectMirror

object JavaScriptResponseHandler extends HandlerFinder[JavaScriptResponseHandler] {
  /**
    *
    * The value of __kind in JS
    * @return
    */
  override val kind: String = "response-handler"

  override protected def extractHandler(jsc: JavaScriptContext, handler: ScriptObjectMirror, as: ArtifactSource, ctx: JavaScriptHandlerContext): Option[JavaScriptResponseHandler] = {
    Some(new JavaScriptResponseHandler(jsc, handler, as, name(handler), description(handler), parameters(handler), tags(handler)))
  }
}

class JavaScriptResponseHandler (jsc: JavaScriptContext,
                                 handler: ScriptObjectMirror,
                                 as: ArtifactSource,
                                 override val name: String,
                                 override val description: String,
                                 override val parameters: Seq[Parameter],
                                 override val tags: Seq[Tag])
  extends ParameterizedHandler
    with ResponseHandler
    with JavaScriptUtils {

  override def handle(response: InstructionResponse, params: ParameterValues): Option[Plan] = {
    Plan.build(invokeMemberFunction(jsc, handler, "handle", response, params))
  }
}
