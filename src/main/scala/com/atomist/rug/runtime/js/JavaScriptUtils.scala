package com.atomist.rug.runtime.js

import javax.script.{ScriptContext, SimpleBindings}

import com.atomist.param.{Parameter, ParameterValue, ParameterValues, Tag}
import com.atomist.rug.{BadPlanException, InvalidRugParameterDefaultValue, InvalidRugParameterPatternException}
import com.atomist.rug.parser.DefaultIdentifierResolver
import com.atomist.rug.spi.Secret
import jdk.nashorn.api.scripting.{ScriptObjectMirror, ScriptUtils}
import jdk.nashorn.internal.runtime.ScriptRuntime

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

/**
  * Stuff for all JavaScript based things
  */
trait JavaScriptUtils {

  /**
    * Invoke the given member of the JavaScript class with these arguments, processing them as appropriate
    *
    * @param member name of the member to invoke
    * @param args   arguments to invoke with. They will be translated into
    *               appropriate JavaScript types if necessary
    * @return result of the invocation
    */
  protected def invokeMemberFunction(jsc: JavaScriptContext, jsVar: ScriptObjectMirror, member: String, params: Option[ParameterValues], args: Object*): Any = {
    jsc.withEnhancedExceptions {
      val clone = cloneVar(jsc, jsVar)
      if(params.nonEmpty){
        setParameters(clone, params.get.parameterValues)
      }
      //TODO wrap parameters in safe committing proxy
      clone.asInstanceOf[ScriptObjectMirror].callMember(member, args: _*)
    }
  }

  /**
    * Separate for test
    */
  private[js] def cloneVar (jsc: JavaScriptContext, objToClone: ScriptObjectMirror) : ScriptObjectMirror = {
    val bindings = new SimpleBindings()
    bindings.put("rug",objToClone)

    //TODO - why do we need this?
    jsc.engine.getContext.getBindings(ScriptContext.ENGINE_SCOPE).asScala.foreach{
      case (k: String, v: AnyRef) => bindings.put(k,v)
    }
    jsc.engine.eval("Object.create(rug);", bindings).asInstanceOf[ScriptObjectMirror]
  }
  /**
    * Make sure we only set fields if they've been decorated with @Parameter or @MappedParameter
    */
  protected def setParameters(clone: ScriptObjectMirror, params: Seq[ParameterValue]): Unit = {
    val decoratedParamNames: Set[String] = clone.get("__parameters") match {
      case ps: ScriptObjectMirror if !ps.isEmpty =>
        ps.asScala.collect {
          case (_, details: ScriptObjectMirror) =>
            details.get("name").asInstanceOf[String]
        }.toSet[String]
      case _ => Set()
    }

    val mappedParams: Set[String] = clone.get("__mappedParameters") match {
      case ps: ScriptObjectMirror if !ps.isEmpty =>
        ps.asScala.collect {
          case (_, details: ScriptObjectMirror) =>
            details.get("localKey").asInstanceOf[String]
        }.toSet[String]
      case _ => Set()
    }
    params.foreach(p => {
      if(decoratedParamNames.contains(p.getName)){
        clone.put(p.getName,p.getValue)
      }
      if(mappedParams.contains(p.getName)){
        clone.put(p.getName,p.getValue)
      }
    })
  }

  /**
    * Fetch a member by name.
    */
  protected def getMember(someVar: ScriptObjectMirror, name: String) : Option[AnyRef] = {
    someVar.getMember(name) match {
      case value if value != ScriptRuntime.UNDEFINED => Some(value)
      case _ => Option.empty
    }
  }

  protected def tags(someVar: ScriptObjectMirror): Seq[Tag] = {
    Try {
      getMember(someVar, "__tags") match {
        case Some(som: ScriptObjectMirror) =>
          val stringValues = som.values().asScala collect {
            case s: String => s
          }
          stringValues.map(s => Tag(s, s)).toSeq
        case _ => Nil
      }
    }.getOrElse(Nil)
  }

  /**
    * Either read the parameters field or look for annotated parameters.
    */
  protected def parameters(someVar: ScriptObjectMirror): Seq[Parameter] = {
    getMember(someVar, "__parameters") match {
      case Some(ps: ScriptObjectMirror) if !ps.isEmpty =>
        ps.asScala.collect {
          case (_, details: ScriptObjectMirror) => parameterVarToParameter(someVar, details)
        }.toSeq
      case _ => Seq()
    }
  }

  /**
    * Fetch any secrets decorated on the handler
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

  private def parameterVarToParameter(rug: ScriptObjectMirror, details: ScriptObjectMirror) : Parameter = {

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

    if(details.hasMember("required")){
      parameter.setRequired(details.get("required").asInstanceOf[Boolean])
    }else{
      parameter.setRequired(true)
    }

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

    if(rug.hasMember(pName) && rug.getMember(pName) != null){
      val asString = rug.getMember(pName).toString
      if (!parameter.isValidValue(asString))
        throw new InvalidRugParameterDefaultValue(s"Parameter $pName default value ($asString) is not valid: $parameter")
      parameter.setDefaultValue(rug.getMember(pName).toString)
    }
    parameter
  }
  protected def name(obj: ScriptObjectMirror): String = {
    if(obj.hasMember("__name")){
      obj.getMember("__name").asInstanceOf[String]
    }else{
      Try(obj.getMember("constructor").asInstanceOf[ScriptObjectMirror].getMember("name").asInstanceOf[String]) match {
        case Success(name) => name
        case Failure(error) => throw new BadPlanException(s"Could not determine name of Rug", error)
      }
    }
  }

  protected def description(obj: ScriptObjectMirror): String = obj.getMember("__description").asInstanceOf[String]

  protected def kind(obj: ScriptObjectMirror): String = obj.getMember("__kind").asInstanceOf[String]
}
