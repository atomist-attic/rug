package com.atomist.rug.runtime.js

import com.atomist.param._
import com.atomist.project.archive.RugResolver
import com.atomist.rug.runtime.plans.MappedParameterSupport
import com.atomist.rug.runtime.{CommandHandler, TestDescriptor}
import com.atomist.rug.spi.Handlers.Plan
import com.atomist.rug.spi.Secret
import com.atomist.rug.{InvalidHandlerResultException, InvalidTestDescriptorException}

/**
  * Finds JavaScriptCommandHandlers in Nashorn vars
  */
class JavaScriptCommandHandlerFinder
  extends JavaScriptRugFinder[JavaScriptCommandHandler]
    with JavaScriptUtils {

  /**
    * Is the supplied thing valid at all?
    */
  def isValid(obj: JavaScriptObject): Boolean = {
    obj.getMember("__kind") == "command-handler" &&
      obj.hasMember("handle") &&
      obj.getMember("handle").asInstanceOf[JavaScriptObject].isFunction
  }

  override def create(jsc: JavaScriptEngine,
                      handler: JavaScriptObject,
                      resolver: Option[RugResolver]): Option[JavaScriptCommandHandler] = {
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
      testDescriptor(handler)))
  }

  /**
    * Extract any test related metadata - if any.
    */
  protected def testDescriptor(someVar: JavaScriptObject): Option[TestDescriptor] = {
    someVar.getMember("__test") match {
      case som: JavaScriptObject => (som.getMember("description"), som.getMember("kind")) match {
        case (description: String, kind: String) => Some(TestDescriptor(kind, description))
        case _ => throw new InvalidTestDescriptorException("A test must have a 'kind' and a 'description'")
      }
      case _ => None
    }
  }
  /**
    * Extract intent from a var.
    */
  protected def intent(someVar: JavaScriptObject): Seq[String] = {
    someVar.getMember("__intent") match {
      case som: JavaScriptObject =>
        val stringValues = som.values().collect {
          case s: String => s
        }
        stringValues
      case _ => Nil
    }
  }

  /**
    * Look for mapped parameters (i.e. defined by the bot).
    *
    * See MappedParameters in Handlers.ts for examples.
    */
  protected def mappedParameters(someVar: JavaScriptObject): Seq[MappedParameter] = {
    someVar.getMember("__mappedParameters") match {
      case ps: JavaScriptObject if !ps.isEmpty =>
        ps.values().collect {
          case details: JavaScriptObject =>
            val localKey = details.getMember("localKey").asInstanceOf[String]
            val foreignKey = details.getMember("foreignKey").asInstanceOf[String]
            MappedParameter(localKey, foreignKey)
        }
      case _ => Seq()
    }
  }
}

/**
  * Runs a CommandHandler in Nashorn.
  */
class JavaScriptCommandHandler(jsc: JavaScriptEngine,
                               handler: JavaScriptObject,
                               override val name: String,
                               override val description: String,
                               override val parameters: Seq[Parameter],
                               override val mappedParameters: Seq[MappedParameter],
                               override val tags: Seq[Tag],
                               override val secrets: Seq[Secret],
                               override val intent: Seq[String] = Seq(),
                               val testDescriptor: Option[TestDescriptor] = None)
  extends CommandHandler
    with MappedParameterSupport
    with JavaScriptUtils {

  /**
    * We expect all mapped parameters to also be passed in with the normal params
    */
  override def handle(ctx: RugContext, params: ParameterValues): Option[Plan] = {
    val validated = addDefaultParameterValues(params)
    validateParameters(validated)
    // We need to proxy the context to allow for property access
    jsc.invokeMember(
      handler,
      "handle",
      Some(validated),
      ctx) match {
      case plan: JavaScriptObject =>
        ConstructPlan(plan, Some(this))
      case other =>
        throw new InvalidHandlerResultException(s"$name CommandHandler did not return a recognized response ($other) when invoked with ${params.toString()}")
    }
  }
}
