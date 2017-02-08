package com.atomist.rug.runtime.js

import scala.collection.JavaConverters._
import com.atomist.param.{Parameter, Tag}
import com.atomist.rug.{InvalidRugParameterDefaultValue, InvalidRugParameterPatternException}
import com.atomist.rug.parser.DefaultIdentifierResolver
import com.atomist.rug.runtime.Handler
import com.atomist.rug.runtime.js.interop.JavaScriptHandlerContext
import com.atomist.source.ArtifactSource
import jdk.nashorn.api.scripting.ScriptObjectMirror

import scala.util.Try

/**
  * Some common things used to extract handlers from nashorn
  */
trait HandlerFinder[T <: Handler] {

  /**
    * The value of __kind in JS
    * @return
    */
  def kind: String
  /**
    * Is the supplied thing valid at all?
    * @param obj
    * @return
    */
  protected def isValidHandler(obj: ScriptObjectMirror): Boolean = {
    obj.hasMember("__kind") && obj.hasMember("__name") && obj.hasMember("__description") && obj.hasMember("handle")
  }

  def extractHandlers(rugAs: ArtifactSource, ctx: JavaScriptHandlerContext): Seq[T] = {
    val jsc = new JavaScriptContext(rugAs)
    jsc.vars.collect {
      case Var(_, handler) if isValidHandler(handler) && handler.getMember("__kind") == kind => extractHandler(jsc, handler, rugAs, ctx)
    }.flatten
  }

  protected def extractHandler(jsc: JavaScriptContext, handler: ScriptObjectMirror, rugAs: ArtifactSource, ctx: JavaScriptHandlerContext): Option[T]

  protected def name(obj: ScriptObjectMirror): String = obj.getMember("__name").asInstanceOf[String]
  protected def description(obj: ScriptObjectMirror): String = obj.getMember("__description").asInstanceOf[String]
  protected def kind(obj: ScriptObjectMirror): String = obj.getMember("__kind").asInstanceOf[String]

  protected def tags(someVar: ScriptObjectMirror): Seq[Tag] = {
    Try {
      someVar.getMember("__tags") match {
        case som: ScriptObjectMirror =>
          val stringValues = som.values().asScala collect {
            case s: String => s
          }
          stringValues.map(s => Tag(s, s)).toSeq
        case _ => Nil
      }
    }.getOrElse(Nil)
  }
  /**
    * Either read the parameters field or look for annotated parameters
    * @return
    */
  protected def parameters(someVar: ScriptObjectMirror) : Seq[Parameter] = {
    someVar.get("__parameters") match {
      case ps: ScriptObjectMirror if !ps.isEmpty =>
        ps.asScala.collect {
          case (_, details: ScriptObjectMirror) => parameterVarToParameter(someVar, details)
        }.toSeq
      case _ => Seq()
    }
  }

  protected def parameterVarToParameter(rug: ScriptObjectMirror, details: ScriptObjectMirror) : Parameter = {

    val pName = details.get("name").asInstanceOf[String]
    val pPattern = details.get("pattern").asInstanceOf[String]
    val parameter = Parameter(pName, pPattern)
    parameter.setDisplayName(details.get("displayName").asInstanceOf[String])

    details.get("maxLength") match {
      case x: AnyRef => parameter.setMaxLength(x.asInstanceOf[Int])
      case _ => parameter.setMaxLength(-1)
    }
    details.get("minLength") match {
      case x: AnyRef => parameter.setMinLength(x.asInstanceOf[Int])
      case _ => parameter.setMinLength(-1)
    }

    parameter.setDefaultRef(details.get("defaultRef").asInstanceOf[String])
    val disp = details.get("displayable")
    parameter.setDisplayable(if(disp != null) disp.asInstanceOf[Boolean] else true)
    parameter.setRequired(details.get("required").asInstanceOf[Boolean])

    parameter.addTags(tags(details))

    parameter.setValidInputDescription(details.get("validInput").asInstanceOf[String])
    parameter.describedAs(details.get("description").asInstanceOf[String])

    pPattern match {
      case s: String if s.startsWith("@") => DefaultIdentifierResolver.resolve(s.substring(1)) match {
        case Left(_) =>
          throw new InvalidRugParameterPatternException(s"Unable to recognize predefined validation pattern for parameter $pName: $s")
        case Right(pat) => parameter.setPattern(pat)
      }
      case s: String if !s.startsWith("^") || !s.endsWith("$") =>
        throw new InvalidRugParameterPatternException(s"Parameter $pName validation pattern must contain anchors: $s")
      case s: String => parameter.setPattern(s)
      case _ => throw new InvalidRugParameterPatternException(s"Parameter $pName has no valid validation pattern")
    }

    details.get("default") match {
      case x: String =>
        if (!parameter.isValidValue(x))
          throw new InvalidRugParameterDefaultValue(s"Parameter $pName default value ($x) is not valid: $parameter")
        parameter.setDefaultValue(x)
      case _ =>
    }
    if(details.get("decorated").asInstanceOf[Boolean] && rug.hasMember(pName)){
      parameter.setDefaultValue(rug.getMember(pName).toString)
    }
    parameter
  }
}

