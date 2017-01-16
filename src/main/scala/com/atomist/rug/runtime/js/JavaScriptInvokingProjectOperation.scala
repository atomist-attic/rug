package com.atomist.rug.runtime.js

import com.atomist.param.{AllowedValue, Parameter, Tag}
import com.atomist.project.common.support.ProjectOperationParameterSupport
import com.atomist.project.{ProjectOperation, ProjectOperationArguments}
import com.atomist.rug.{InvalidRugParameterDefaultValue, InvalidRugParameterPatternException}
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.parser.DefaultIdentifierResolver
import com.atomist.rug.runtime.js.interop.SafeCommittingProxy
import com.atomist.rug.runtime.rugdsl.ContextAwareProjectOperation
import com.atomist.rug.spi.TypeRegistry
import com.atomist.source.ArtifactSource
import com.typesafe.scalalogging.LazyLogging
import jdk.nashorn.api.scripting.{ScriptObjectMirror, ScriptUtils}

import scala.collection.JavaConverters._
import scala.util.Try

/**
  * Superclass for all operations that delegate to JavaScript.
  *
  * @param jsc       JavaScript context
  * @param jsVar     var reference in Nashorn
  * @param rugAs     backing artifact source for the Rug archive
  */
abstract class JavaScriptInvokingProjectOperation(
                                                   jsc: JavaScriptContext,
                                                   jsVar: ScriptObjectMirror,
                                                   rugAs: ArtifactSource
                                                 )
  extends ProjectOperationParameterSupport
    with ContextAwareProjectOperation
    with LazyLogging {

  private val typeRegistry: TypeRegistry = DefaultTypeRegistry

  private val projectType = typeRegistry.findByName("Project")
    .getOrElse(throw new TypeNotPresentException("Project", null))

  readTagsFromMetadata(jsVar).foreach(t => addTag(t))

  readParametersFromMetadata.foreach(p => addParameter(p))

  private var _context: Seq[ProjectOperation] = Nil

  override def setContext(ctx: Seq[ProjectOperation]): Unit = {
    _context = ctx
  }

  protected def context: Seq[ProjectOperation] = {
    _context
  }

  override def description: String = jsVar.getMember("description").asInstanceOf[String] match {
    case s: String => s
    case _ => name
  }

  /**
    * Invoke the given member of the JavaScript class with these arguments, processing them as appropriate
    *
    * @param member name of the member to invoke
    * @param args   arguments to invoke with. They will be translated into
    *               appropriate JavaScript types if necessary
    * @return result of the invocation
    */
  protected def invokeMemberWithParameters(member: String, args: Object*): Any = {
    // Translate parameters if necessary
    val processedArgs = args.foldLeft(Seq[Object]())(
      (acc: Seq[Object], cur: Object) => cur match {
        case poa: ProjectOperationArguments =>
          acc :+ poa.parameterValues.map(p => p.getName -> p.getValue).toMap.asJava
        case x => acc :+ x
      }
    )
    jsVar.callMember(member,processedArgs: _* )
  }

  protected def readTagsFromMetadata(someVar: ScriptObjectMirror): Seq[Tag] = {
    Try {
      someVar.getMember("tags") match {
        case som: ScriptObjectMirror =>
          val stringValues = som.values().asScala collect {
            case s: String => s
          }
          stringValues.map(s => Tag(s, s)).toSeq
        case _ => Nil
      }
    }.getOrElse(Nil)
  }

  protected def readParametersFromMetadata: Seq[Parameter] = {

    val pvar = jsVar.get("parameters").asInstanceOf[ScriptObjectMirror]
    if(pvar == null || pvar.asScala.isEmpty){
      return Nil
    }
    val values = pvar.asScala.collect {
      case (_, _details: AnyRef) =>
        val details = _details.asInstanceOf[ScriptObjectMirror]

        val pName = details.get("name").asInstanceOf[String]
        val pPattern = details.get("pattern").asInstanceOf[String]
        val p = Parameter(pName, pPattern)
        p.setDisplayName(details.get("displayName").asInstanceOf[String])

        details.get("maxLength") match {
          case x: AnyRef => p.setMaxLength(x.asInstanceOf[Int])
          case _ => p.setMaxLength(-1)
        }
        details.get("minLength") match {
          case x: AnyRef => p.setMinLength(x.asInstanceOf[Int])
          case _ => p.setMinLength(-1)
        }

        p.setDefaultRef(details.get("defaultRef").asInstanceOf[String])
        val disp = details.get("displayable")
        p.setDisplayable(if(disp != null) disp.asInstanceOf[Boolean] else true)
        p.setRequired(details.get("required").asInstanceOf[Boolean])

        p.addTags(readTagsFromMetadata(details))

        p.setValidInputDescription(details.get("validInput").asInstanceOf[String])
        p.describedAs(details.get("description").asInstanceOf[String])

        pPattern match {
          case s: String if s.startsWith("@") => DefaultIdentifierResolver.resolve(s.substring(1)) match {
            case Left(_) =>
              throw new InvalidRugParameterPatternException(s"Unable to recognize predefined validation pattern for parameter $pName: $s")
            case Right(pat) => p.setPattern(pat)
          }
          case s: String if !s.startsWith("^") || !s.endsWith("$") =>
            throw new InvalidRugParameterPatternException(s"Parameter $pName validation pattern must contain anchors: $s")
          case s: String => p.setPattern(s)
          case _ => throw new InvalidRugParameterPatternException(s"Parameter $pName has no valid validation pattern")
        }

        details.get("default") match {
          case x: String =>
            if (!p.isValidValue(x))
              throw new InvalidRugParameterDefaultValue(s"Parameter $pName default value ($x) is not valid: $p")
            p.setDefaultValue(x)
          case _ =>
        }

        p
    }
    values.toSeq
  }

  /**
    * Convenient class allowing subclasses to wrap projects in a safe, updating proxy
    *
    * @param pmv project to wrap
    * @return proxy TypeScript callers can use
    */
  protected def wrapProject(pmv: ProjectMutableView): SafeCommittingProxy = {
    new SafeCommittingProxy(projectType, pmv)
  }

}
