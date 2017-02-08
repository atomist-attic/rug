package com.atomist.rug.runtime.js

import com.atomist.param.{Parameter, ParameterValues, Tag}
import com.atomist.rug.runtime.js.interop.JavaScriptHandlerContext
import com.atomist.rug.runtime.{CommandContext, CommandHandler}
import com.atomist.rug.spi.Plan.Plan
import com.atomist.source.ArtifactSource
import jdk.nashorn.api.scripting.ScriptObjectMirror

import scala.collection.JavaConverters._

/**
  * Finds JavaScriptCommandHandlers in Nashorn vars
  */
object JavaScriptCommandHandler
  extends HandlerFinder[JavaScriptCommandHandler] {

  override def kind = "command-handler"

  override def extractHandler(jsc: JavaScriptContext, handler: ScriptObjectMirror, as: ArtifactSource, ctx: JavaScriptHandlerContext): Option[JavaScriptCommandHandler] = {
    Some(new JavaScriptCommandHandler(jsc, handler, as, name(handler), description(handler), parameters(handler), tags(handler), intent(handler)))
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
  * @param rugAs
  * @param name
  * @param description
  * @param parameters
  * @param tags
  * @param intent
  */
class JavaScriptCommandHandler(jsc: JavaScriptContext,
                               handler: ScriptObjectMirror,
                               rugAs: ArtifactSource,
                               override val name: String,
                               override val description: String,
                               override val parameters: Seq[Parameter],
                               override val tags: Seq[Tag],
                               override val intent: Seq[String] = Seq())
  extends CommandHandler
  with JavaScriptUtils {

  override def handle(ctx: CommandContext, params: ParameterValues): Option[Plan] = {
    //TODO integrate proper factory
    Plan.build(invokeMemberFunction(jsc, handler, "handle", ctx, params))
  }
}
