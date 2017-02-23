package com.atomist.rug.runtime.js

import com.atomist.param.{Parameter, ParameterValues, ParameterizedSupport, Tag}
import com.atomist.rug.InvalidHandlerResultException
import com.atomist.rug.runtime.{InstructionResponse, ParameterizedRug, ResponseHandler}
import com.atomist.rug.spi.Handlers.Plan
import jdk.nashorn.api.scripting.ScriptObjectMirror

/**
  * Extract response handlers from a Nashorn instance
  */
class JavaScriptResponseHandlerFinder
  extends BaseJavaScriptHandlerFinder[JavaScriptResponseHandler] {
  /**
    *
    * The value of kind in JS
    * @return
    */
  override val kind: String = "response-handler"

  override protected def extractHandler(jsc: JavaScriptContext, handler: ScriptObjectMirror): Option[JavaScriptResponseHandler] = {
    Some(new JavaScriptResponseHandler(jsc, handler, name(handler), description(handler), parameters(handler), tags(handler)))
  }
}

class JavaScriptResponseHandler (jsc: JavaScriptContext,
                                 handler: ScriptObjectMirror,
                                 override val name: String,
                                 override val description: String,
                                 override val parameters: Seq[Parameter],
                                 override val tags: Seq[Tag])
  extends ParameterizedRug
    with ResponseHandler
    with ParameterizedSupport
    with JavaScriptUtils {

  override def handle(response: InstructionResponse, params: ParameterValues): Option[Plan] = {
    //TODO this handle method is almost identical to the command handler - extract it
    val validated = addDefaultParameterValues(params)
    validateParameters(validated)
    invokeMemberFunction(jsc, handler, "handle", response, validated) match {
      case plan: ScriptObjectMirror => ConstructPlan(plan)
      case other => throw new InvalidHandlerResultException(s"$name ResponseHandler did not return a recognized response ($other) when invoked with ${params.toString()}")
    }
  }
}
