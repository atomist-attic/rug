package com.atomist.rug.runtime.js

import com.atomist.param._
import com.atomist.project.archive.RugResolver
import com.atomist.rug.runtime.RugScopes.Scope
import com.atomist.rug.runtime.plans.{JsonResponseConverter, NullResponseConverter, ResponseConverter}
import com.atomist.rug.runtime.{ParameterizedRug, ResponseHandler}
import com.atomist.rug.spi.ExportFunction
import com.atomist.rug.spi.Handlers.{Plan, Response}
import com.atomist.rug.{InvalidHandlerException, InvalidHandlerResultException}

import scala.annotation.meta.getter

/**
  * Extract response handlers from a Nashorn instance
  */
class JavaScriptResponseHandlerFinder
  extends JavaScriptRugFinder[JavaScriptResponseHandler]
    with JavaScriptUtils {

  override def create(jsc: JavaScriptEngine, handler: JavaScriptObject, resolver: Option[RugResolver]): Option[JavaScriptResponseHandler] = {
    Some(new JavaScriptResponseHandler(
      jsc,
      handler,
      name(handler),
      description(handler),
      parameters(handler),
      tags(handler),
      coerce(jsc, handler),
      scope(handler)))
  }

  /**
    * Figure out if we need to parse json or whatever.
    */
  def coerce(jsc: JavaScriptEngine, handler: JavaScriptObject): ResponseConverter = {
    handler.getMember("__coercion") match {
      case "JSON" => new JsonResponseConverter(jsc, handler)
      case str: String => throw new InvalidHandlerException(s"Don't know how to coerce responses to $str")
      case _ => NullResponseConverter
    }
  }

  override def isValid(obj: JavaScriptObject): Boolean = {
    obj.getMember("__kind") == "response-handler" &&
      obj.hasMember("handle") &&
      obj.getMember("handle").asInstanceOf[JavaScriptObject].isFunction
  }
}

class JavaScriptResponseHandler(jsc: JavaScriptEngine,
                                handler: JavaScriptObject,
                                override val name: String,
                                override val description: String,
                                override val parameters: Seq[Parameter],
                                override val tags: Seq[Tag],
                                responseCoercer: ResponseConverter,
                                override val scope: Scope)
  extends ParameterizedRug
    with ResponseHandler
    with ParameterizedSupport
    with JavaScriptUtils {

  override def handle(ctx: RugContext, response: Response, params: ParameterValues): Option[Plan] = {
    // TODO this handle method is almost identical to the command handler - extract it
    val validated = addDefaultParameterValues(params)
    validateParameters(validated)
    val coerced = responseCoercer.convert(response)
    jsc.invokeMember(
      handler,
      "handle",
      Some(validated),
      jsResponse(coerced.msg.orNull, coerced.code.getOrElse(-1), coerced.body.getOrElse(Nil)),
      ctx) match {
      case plan: JavaScriptObject => ConstructPlan(plan, Some(this))
      case other => throw new InvalidHandlerResultException(s"$name ResponseHandler did not return a recognized response ($other) when invoked with ${params.toString()}")
    }
  }
}

private case class jsResponse(@(ExportFunction @getter)(description = "Response message", readOnly = true, exposeAsProperty = true)
                               msg: String,
                              @(ExportFunction @getter)(description = "Response code", readOnly = true, exposeAsProperty = true)
                              code: Int,
                              @(ExportFunction @getter)(description = "Response body", readOnly = true, exposeAsProperty = true)
                              body: AnyRef)
