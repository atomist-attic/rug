package com.atomist.rug.runtime.js

import com.atomist.param.{Parameter, ParameterValues, ParameterizedSupport, Tag}
import com.atomist.project.archive.RugResolver
import com.atomist.rug.runtime.js.interop.jsScalaHidingProxy
import com.atomist.rug.{InvalidHandlerException, InvalidHandlerResultException}
import com.atomist.rug.runtime.plans.{JsonResponseCoercer, NullResponseCoercer, ResponseCoercer}
import com.atomist.rug.runtime.{ParameterizedRug, ResponseHandler, _}
import com.atomist.rug.spi.Handlers.{Plan, Response}
import jdk.nashorn.api.scripting.ScriptObjectMirror

/**
  * Extract response handlers from a Nashorn instance
  */
class JavaScriptResponseHandlerFinder
  extends JavaScriptRugFinder[JavaScriptResponseHandler]
  with JavaScriptUtils{

  override def create(jsc: JavaScriptContext, handler: ScriptObjectMirror, resolver: Option[RugResolver]): Option[JavaScriptResponseHandler] = {
    Some(new JavaScriptResponseHandler(jsc, handler, name(handler), description(handler), parameters(handler), tags(handler), coerce(jsc,handler)))
  }

  /**
    * Figure out if we need to parse json or whatever
    * @param handler
    * @return
    */
  def coerce(jsc: JavaScriptContext, handler: ScriptObjectMirror): ResponseCoercer = {
    handler.getMember("__coercion") match {
      case "JSON" => new JsonResponseCoercer(jsc, handler)
      case str: String => throw new InvalidHandlerException(s"Don't know how to coerce responses to $str")
      case _ => NullResponseCoercer
    }
  }

  override def isValid(obj: ScriptObjectMirror): Boolean = {
    obj.getMember("__kind") == "response-handler" &&
      obj.hasMember("handle") &&
      obj.getMember("handle").asInstanceOf[ScriptObjectMirror].isFunction
  }
}

class JavaScriptResponseHandler (jsc: JavaScriptContext,
                                 handler: ScriptObjectMirror,
                                 override val name: String,
                                 override val description: String,
                                 override val parameters: Seq[Parameter],
                                 override val tags: Seq[Tag],
                                 responseCoercer: ResponseCoercer)
  extends ParameterizedRug
    with ResponseHandler
    with ParameterizedSupport
    with JavaScriptUtils {

  override def handle(response: Response, params: ParameterValues): Option[Plan] = {
    //TODO this handle method is almost identical to the command handler - extract it
    val validated = addDefaultParameterValues(params)
    validateParameters(validated)
    val coerced = responseCoercer.coerce(response)
    invokeMemberFunction(
      jsc,
      handler,
      "handle",
      Some(validated),
      jsScalaHidingProxy(jsResponse(coerced.msg.orNull, coerced.code.getOrElse(-1), coerced.body.getOrElse(Nil)))) match {
      case plan: ScriptObjectMirror => ConstructPlan(plan, Some(this))
      case other => throw new InvalidHandlerResultException(s"$name ResponseHandler did not return a recognized response ($other) when invoked with ${params.toString()}")
    }
  }
}

private case class jsResponse(msg: String,
                              code: Int,
                              body: AnyRef)
