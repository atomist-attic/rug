package com.atomist.rug.runtime.js

import com.atomist.param.{Parameter, ParameterValues, ParameterizedSupport, Tag}
import com.atomist.rug.InvalidHandlerResultException
import com.atomist.rug.runtime.js.interop.JavaScriptHandlerContext
import com.atomist.rug.runtime.{CommandContext, CommandHandler, ParameterizedRug}
import com.atomist.rug.spi.Handlers.Plan
import jdk.nashorn.api.scripting.ScriptObjectMirror

import scala.collection.JavaConverters._

/**
  * Finds JavaScriptCommandHandlers in Nashorn vars
  */
class JavaScriptCommandHandlerFinder(ctx: JavaScriptHandlerContext)
  extends BaseJavaScriptHandlerFinder[JavaScriptCommandHandler] (ctx) {

  override def kind = "command-handler"

  override def extractHandler(jsc: JavaScriptContext, handler: ScriptObjectMirror, ctx: JavaScriptHandlerContext): Option[JavaScriptCommandHandler] = {
    Some(new JavaScriptCommandHandler(jsc, handler, name(handler), description(handler), parameters(handler), tags(handler), intent(handler)))
  }

  /**
    * Extract intent from a var
    *
    * @param someVar
    * @return
    */
  protected def intent(someVar: ScriptObjectMirror): Seq[String] = {
    someVar.getMember("__intent") match {
      case som: ScriptObjectMirror =>
        val stringValues = som.values().asScala collect {
          case s: String => s
        }
        stringValues.toSeq
    }
  }
}

/**
  * Runs a CommandHandler in Nashorn
  *
  * @param handler
  * @param name
  * @param description
  * @param parameters
  * @param tags
  * @param intent
  */
class JavaScriptCommandHandler(jsc: JavaScriptContext,
                               handler: ScriptObjectMirror,
                               override val name: String,
                               override val description: String,
                               parameters: Seq[Parameter],
                               override val tags: Seq[Tag],
                               override val intent: Seq[String] = Seq())
  extends CommandHandler
  with ParameterizedRug
  with ParameterizedSupport
  with JavaScriptUtils {

  addParameters(parameters)

  override def handle(ctx: CommandContext, params: ParameterValues): Option[Plan] = {
    val validated = addDefaultParameterValues(params)
    validateParameters(validated)
    invokeMemberFunction(jsc, handler, "handle", ctx, params) match {
      case plan: ScriptObjectMirror => ConstructPlan(plan)
      case other => throw new InvalidHandlerResultException(s"$name CommandHandler did not return a recognized response ($other) when invoked with ${params.toString()}")
    }
  }
}