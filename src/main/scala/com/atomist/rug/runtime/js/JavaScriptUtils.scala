package com.atomist.rug.runtime.js

import com.atomist.param.{Parameter, ParameterValue, Tag}
import com.atomist.rug.parser.DefaultIdentifierResolver
import com.atomist.rug.runtime.RugScopes
import com.atomist.rug.runtime.RugScopes.Scope
import com.atomist.rug.spi.Secret
import com.atomist.rug.{BadPlanException, InvalidRugParameterDefaultValue, InvalidRugParameterPatternException}

import scala.util.{Failure, Success, Try}

/**
  * Stuff for all JavaScript based things
  */
trait JavaScriptUtils {

  /**
    * Make sure we only set fields if they've been decorated with @Parameter or @MappedParameter
    */
  protected def setParameters(obj: JavaScriptObject, params: Seq[ParameterValue]): Unit = {
    val decoratedParamNames: Set[String] = obj.getMember("__parameters") match {
      case ps: JavaScriptObject if !ps.isEmpty =>
        ps.entries().collect {
          case (_, details: JavaScriptObject) =>
            details.getMember("name").asInstanceOf[String]
        }.toSet[String]
      case _ => Set()
    }

    val mappedParams: Set[String] = obj.getMember("__mappedParameters") match {
      case ps: JavaScriptObject if !ps.isEmpty =>
        ps.entries().collect {
          case (_, details: JavaScriptObject) =>
            details.getMember("localKey").asInstanceOf[String]
        }.toSet[String]
      case _ => Set()
    }
    params.foreach(p => {
      if (decoratedParamNames.contains(p.getName)) {
        obj.setMember(p.getName, p.getValue)
      }
      if (mappedParams.contains(p.getName)) {
        obj.setMember(p.getName, p.getValue)
      }
    })
  }

  /**
    * Fetch a member by name.
    */
  protected def getMember(someVar: JavaScriptObject, name: String): Option[AnyRef] = {
    someVar.getMember(name) match {
      case value if value != UNDEFINED => Some(value)
      case _ => Option.empty
    }
  }

  protected def tags(someVar: JavaScriptObject): Seq[Tag] = {
    Try {
      getMember(someVar, "__tags") match {
        case Some(som: JavaScriptObject) =>
          val stringValues = som.values().collect {
            case s: String => s
          }
          stringValues.map(s => Tag(s, s))
        case _ => Nil
      }
    }.getOrElse(Nil)
  }

  /**
    * Either read the parameters field or look for annotated parameters.
    */
  protected def parameters(someVar: JavaScriptObject): Seq[Parameter] = {
    getMember(someVar, "__parameters") match {
      case Some(ps: JavaScriptObject) if !ps.isEmpty =>
        ps.entries().collect {
          case (_, details: JavaScriptObject) => parameterVarToParameter(someVar, details)
        }.toSeq
      case _ => Seq()
    }
  }

  /**
    * Fetch any secrets decorated on the handler
    */
  protected def secrets(someVar: JavaScriptObject): Seq[Secret] = {
    someVar.getMember("__secrets") match {
      case som: JavaScriptObject =>
        som.values().collect {
          case s: String => Secret(s, s)
        }
      case _ => Nil
    }
  }

  private def parameterVarToParameter(rug: JavaScriptObject, details: JavaScriptObject): Parameter = {

    val pName = details.getMember("name").asInstanceOf[String]
    val pPattern = details.getMember("pattern").asInstanceOf[String]
    val parameter = Parameter(pName, pPattern)
    details.getMember("displayName") match {
      case o: String => parameter.setDisplayName(o)
      case _ =>
    }

    details.getMember("maxLength") match {
      case x: AnyRef if x.isInstanceOf[UNDEFINED] => parameter.setMaxLength(x.asInstanceOf[Int])
      case _ => parameter.setMaxLength(-1)
    }
    details.getMember("minLength") match {
      case x: AnyRef if x.isInstanceOf[UNDEFINED] => parameter.setMinLength(x.asInstanceOf[Int])
      case _ => parameter.setMinLength(-1)
    }

    details.getMember("defaultRef") match {
      case x: String => parameter.setDefaultRef(x)
      case _ =>
    }

    details.getMember("displayable") match {
      case o if o.isInstanceOf[Boolean] => parameter.setDisplayable(o.asInstanceOf[Boolean])
      case _ => parameter.setDisplayable(true)
    }

    if (details.hasMember("required")) {
      parameter.setRequired(details.getMember("required").asInstanceOf[Boolean])
    } else {
      parameter.setRequired(true)
    }

    parameter.addTags(tags(details))
    details.getMember("validInput") match {
      case o: String => parameter.setValidInputDescription(o)
      case _ =>
    }
    parameter.describedAs(details.getMember("description").asInstanceOf[String])

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

    if (rug.hasMember(pName) && rug.getMember(pName) != null) {
      val asString = rug.getMember(pName).toString
      if (!parameter.isValidValue(asString))
        if (asString == "") {
           // how can we warn them??? this is deprecated
          println(s"WARNING: On rug ${name(rug)}, parameter '$pName' has a default value of '$asString', but that is not a valid value for this parameter. Set the parameter to null instead, please.")
        } else {
          throw new InvalidRugParameterDefaultValue(s"Parameter $pName default value ($asString) is not valid: $parameter")
        }
      parameter.setDefaultValue(rug.getMember(pName).toString)
    }

    parameter
  }

  protected def name(obj: JavaScriptObject): String = {
    if (obj.hasMember("__name")) {
      obj.getMember("__name").asInstanceOf[String]
    } else {
      Try(obj.getMember("constructor").asInstanceOf[JavaScriptObject].getMember("name").asInstanceOf[String]) match {
        case Success(name) => name
        case Failure(error) => throw new BadPlanException(s"Could not determine name of Rug", error)
      }
    }
  }

  protected def scope(obj: JavaScriptObject): Scope = {
    obj.getMember("__scope") match {
      case o: String => RugScopes.from(o)
      case _ => RugScopes.DEFAULT
    }
  }

  protected def description(obj: JavaScriptObject): String = obj.getMember("__description").asInstanceOf[String]

  protected def kind(obj: JavaScriptObject): String = obj.getMember("__kind").asInstanceOf[String]
}
