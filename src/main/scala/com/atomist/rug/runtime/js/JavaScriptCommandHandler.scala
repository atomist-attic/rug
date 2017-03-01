package com.atomist.rug.runtime.js

import com.atomist.param._
import com.atomist.rug.InvalidHandlerResultException
import com.atomist.rug.runtime.plans.MappedParameterSupport
import com.atomist.rug.runtime.{AddressableRug, CommandHandler, RugSupport}
import com.atomist.rug.spi.Handlers.Plan
import com.atomist.rug.spi.Secret
import jdk.nashorn.api.scripting.ScriptObjectMirror

import scala.collection.JavaConverters._

/**
  * Finds JavaScriptCommandHandlers in Nashorn vars
  */
class JavaScriptCommandHandlerFinder
  extends BaseJavaScriptHandlerFinder[JavaScriptCommandHandler] {

  override def kind = "command-handler"

  override def extractHandler(jsc: JavaScriptContext, handler: ScriptObjectMirror, externalContext: Seq[AddressableRug]): Option[JavaScriptCommandHandler] = {
    Some(new JavaScriptCommandHandler(
      jsc,
      handler,
      name(handler),
      description(handler),
      parameters(handler),
      mappedParameters(handler),
      tags(handler),
      secrets(handler),
      intent(handler),
      externalContext))
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
      case _ => Nil
    }
  }

  /**
    * Look for mapped parameters (i.e. defined by the bot).
    *
    * See MappedParameters in Handlers.ts for examples.
    *
    * @return
    */
  protected def mappedParameters(someVar: ScriptObjectMirror): Seq[MappedParameter] = {
    getMember(someVar, Seq("__mappedParameters")) match {
      case Some(ps: ScriptObjectMirror) if !ps.isEmpty =>
        ps.asScala.collect {
          case (_, details: ScriptObjectMirror) =>
            val localKey = details.getMember("localKey").asInstanceOf[String]
            val foreignKey = details.getMember("foreignKey").asInstanceOf[String]
            MappedParameter(localKey,foreignKey)
        }.toSeq
      case _ => Seq()
    }
  }
  /**
    * Fetch any secrets decorated on the handler
    * @param someVar
    * @return
    */
  protected def secrets(someVar: ScriptObjectMirror) : Seq[Secret] = {
    someVar.getMember("__secrets") match {
      case som: ScriptObjectMirror =>
        som.values().asScala.collect {
          case s: String => Secret(s,s)
        }.toSeq
      case _ => Nil
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
                               override val parameters: Seq[Parameter],
                               override val mappedParameters: Seq[MappedParameter],
                               override val tags: Seq[Tag],
                               override val secrets: Seq[Secret],
                               override val intent: Seq[String] = Seq(),
                               override val externalContext: Seq[AddressableRug])
  extends CommandHandler
  with MappedParameterSupport
  with RugSupport
  with JavaScriptUtils {

  /**
    * We expect all mapped parameters to also be passed in with the normal params
    * @param ctx
    * @param params
    * @return
    */
  override def handle(ctx: RugContext, params: ParameterValues): Option[Plan] = {
    val validated = addDefaultParameterValues(params)
    validateParameters(validated)
    invokeMemberFunction(jsc, handler, "handle", ctx, validated) match {
      case plan: ScriptObjectMirror => ConstructPlan(plan)
      case other => throw new InvalidHandlerResultException(s"$name CommandHandler did not return a recognized response ($other) when invoked with ${params.toString()}")
    }
  }
}
