package com.atomist.rug.runtime.js

import javax.script.{ScriptContext, SimpleBindings}

import com.atomist.param.{Parameter, ParameterValues, Tag}
import com.atomist.project.ProjectOperation
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.parser.DefaultIdentifierResolver
import com.atomist.rug.runtime.{AddressableRug, RugSupport}
import com.atomist.rug.runtime.js.interop.{PathExpressionException, jsSafeCommittingProxy}
import com.atomist.rug.{InvalidRugParameterDefaultValue, InvalidRugParameterPatternException, RugRuntimeException}
import com.atomist.source.ArtifactSource
import com.typesafe.scalalogging.LazyLogging
import jdk.nashorn.api.scripting.ScriptObjectMirror

import scala.collection.JavaConverters._
import scala.util.Try
import scala.util.control.NonFatal

/**
  * Superclass for all operations that delegate to JavaScript.
  *
  * @param _jsc   JavaScript context
  * @param _jsVar var reference in Nashorn
  * @param rugAs  backing artifact source for the Rug archive
  */
abstract class JavaScriptProjectOperation(
                                           _jsc: JavaScriptContext,
                                           _jsVar: ScriptObjectMirror,
                                           rugAs: ArtifactSource,
                                           override val externalContext: Seq[AddressableRug]
                                         )
  extends ProjectOperation
    with LazyLogging
    with JavaScriptUtils
    with RugSupport{

  /** Needed by BDD testing support */
  val jsVar: ScriptObjectMirror = _jsVar

  // Visible for test
  private[js] val jsc = _jsc

  override def tags: Seq[Tag] = tags(jsVar, Seq("__tags", "tags"))

  override def parameters: Seq[Parameter] = parameters(jsVar, Seq("__parameters", "parameters"))

  override def name: String = getMember(jsVar, Seq("__name", "name")).get.asInstanceOf[String]

  override def description: String = getMember(jsVar, Seq("__description", "description")).get.asInstanceOf[String]

  /**
    * Convenience method that will try `__name` first for decorated things
    */
  protected def getMember(name: String, someVar: ScriptObjectMirror = jsVar): AnyRef = {
    val decorated = s"__$name"
    if (someVar.hasMember(decorated)) {
      someVar.getMember(decorated)
    } else {
      someVar.getMember(name)
    }
  }

  /**
    * Invoke the given member of the JavaScript class with these arguments, processing them as appropriate
    *
    * @param member name of the member to invoke
    * @param args   arguments to invoke with. They will be translated into
    *               appropriate JavaScript types if necessary
    * @return result of the invocation
    */
  protected def invokeMemberWithParameters(member: String, args: Object*): Any =
    jsc.withEnhancedExceptions {
      val clone = cloneVar(jsVar)

      // Translate parameters if necessary
      val processedArgs = args.collect {
        case poa: ParameterValues =>
          val params = poa.parameterValues.map(p => p.getName -> p.getValue).toMap.asJava
          setParamsIfDecorated(clone, params)
          params
        case x => x
      }

      val som = clone.asInstanceOf[ScriptObjectMirror]
      try {
        som.callMember(member, processedArgs: _*)
      } catch {
        case NonFatal(e) =>
          if (!som.hasMember(member)) {
            throw new RugRuntimeException(null, s"Could not invoke member $member on $jsVar, because it doesn't have it", e)
          }
          e.getCause match {
            case pxe: PathExpressionException =>
              throw pxe
            case _ =>
              throw e
          }
      }
    }

  /**
    * Separate for test
    */
  private[js] def cloneVar(jsVar: ScriptObjectMirror): ScriptObjectMirror = {
    val bindings = new SimpleBindings()
    bindings.put("rug", jsVar)

    //TODO - why do we need this?
    jsc.engine.getContext.getBindings(ScriptContext.ENGINE_SCOPE).asScala.foreach {
      case (k: String, v: AnyRef) => bindings.put(k, v)
    }
    jsc.engine.eval("Object.create(rug);", bindings).asInstanceOf[ScriptObjectMirror]
  }

  /**
    * Make sure we only set fields if they've been decorated with @parameter
    */
  private def setParamsIfDecorated(clone: ScriptObjectMirror, params: java.util.Map[String, AnyRef]): Unit = {
    val decoratedParamNames: Set[String] = clone.get("__parameters") match {
      case ps: ScriptObjectMirror if !ps.isEmpty =>
        ps.asScala.collect {
          case (_, details: ScriptObjectMirror) =>
            details.get("name").asInstanceOf[String]
        }.toSet[String]
      case _ => Set()
    }
    params.asScala.foreach {
      case (k: String, v: AnyRef) =>
        if (decoratedParamNames.contains(k)) {
          clone.put(k, v)
        }
    }
  }

  protected def readTagsFromMetadata(someVar: ScriptObjectMirror): Seq[Tag] = {
    Try {
      getMember("tags", someVar) match {
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
    *
    * @return
    */
  protected def readParametersFromMetadata: Seq[Parameter] = {
    getMember("parameters") match {
      case ps: ScriptObjectMirror if !ps.isEmpty =>
        ps.asScala.collect {
          case (_, details: ScriptObjectMirror) => parameterVarToParameter(jsVar, details)
        }.toSeq
      case _ => Seq()
    }
  }

  protected def parameterVarToParameter(rug: ScriptObjectMirror, details: ScriptObjectMirror): Parameter = {

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
    parameter.setDisplayable(if (disp != null) disp.asInstanceOf[Boolean] else true)

    if (details.hasMember("required")) {
      parameter.setRequired(details.get("required").asInstanceOf[Boolean])
    } else {
      parameter.setRequired(true)
    }

    parameter.addTags(readTagsFromMetadata(details))

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
    if (details.get("decorated").asInstanceOf[Boolean] && rug.hasMember(pName)) {
      parameter.setDefaultValue(rug.getMember(pName).toString)
    }
    parameter
  }

  /**
    * Convenient class allowing subclasses to wrap projects in a safe, updating proxy
    *
    * @param pmv project to wrap
    * @return proxy TypeScript callers can use
    */
  protected def wrapProject(pmv: ProjectMutableView): jsSafeCommittingProxy = {
    new jsSafeCommittingProxy(pmv, DefaultTypeRegistry)
  }
}
