package com.atomist.rug.runtime.js

import javax.script.{ScriptContext, SimpleBindings}

import com.atomist.param.{Parameter, ParameterValues, Tag}
import com.atomist.rug.{InvalidRugParameterDefaultValue, InvalidRugParameterPatternException}
import com.atomist.rug.parser.DefaultIdentifierResolver
import jdk.nashorn.api.scripting.ScriptObjectMirror

import scala.collection.JavaConverters._
import scala.util.Try

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
  protected def invokeMemberFunction(jsc: JavaScriptContext, jsVar: ScriptObjectMirror, member: String, args: Object*): Any = {

    val clone = cloneVar(jsc, jsVar)

    // Translate parameters if necessary
    val processedArgs = args.collect {
      case args: ParameterValues =>
        val params = args.parameterValues.map(p => p.getName -> p.getValue).toMap
        setParameters(clone,params)
        params.asJava
      case x => x
    }
    //TODO wrap parameters in safe committing proxy
    clone.asInstanceOf[ScriptObjectMirror].callMember(member,processedArgs: _* )
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
    * Make sure we only set fields if they've been decorated with @parameter
    */
  protected def setParameters(clone: ScriptObjectMirror, params: Map[String, AnyRef]): Unit = {
    val decoratedParamNames: Set[String] = clone.get("__parameters") match {
      case ps: ScriptObjectMirror if !ps.isEmpty =>
        ps.asScala.collect {
          case (_, details: ScriptObjectMirror) =>
            details.get("name").asInstanceOf[String]
        }.toSet[String]
      case _ => Set()
    }
    params.foreach {
      case (k: String, v: AnyRef) =>
        if(decoratedParamNames.contains(k)){
          clone.put(k,v)
        }
    }
  }

  /**
    * Fetch a member by name
    * @param someVar
    * @param names
    * @return
    */
  protected def getMember(someVar: ScriptObjectMirror, names: Seq[String]) : Option[AnyRef] = {
    names.find(someVar.hasMember) match {
      case Some(name) => Some(someVar.getMember(name))
      case _ => Option.empty
    }
  }

  protected def tags(someVar: ScriptObjectMirror, names: Seq[String] = Seq("tags")): Seq[Tag] = {
    Try {
      getMember(someVar, names) match {
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
    * Either read the parameters field or look for annotated parameters
    * @return
    */
  protected def parameters(someVar: ScriptObjectMirror, names: Seq[String] = Seq("parameters")): Seq[Parameter] = {
    getMember(someVar, names) match {
      case Some(ps: ScriptObjectMirror) if !ps.isEmpty =>
        ps.asScala.collect {
          case (_, details: ScriptObjectMirror) => parameterVarToParameter(someVar, details)
        }.toSeq
      case _ => Seq()
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

    details.get("default") match {
      case x: String =>
        if (!parameter.isValidValue(x))
          throw new InvalidRugParameterDefaultValue(s"Parameter $pName default value ($x) is not valid: $parameter")
        parameter.setDefaultValue(x)
      case _ =>
    }
    if(rug.hasMember(pName)){
      parameter.setDefaultValue(rug.getMember(pName).toString)
    }
    parameter
  }
}
