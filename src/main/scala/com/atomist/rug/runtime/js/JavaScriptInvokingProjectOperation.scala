package com.atomist.rug.runtime.js

import javax.script.ScriptContext

import com.atomist.param.{Parameter, ParameterValue, Tag}
import com.atomist.project.common.support.ProjectOperationParameterSupport
import com.atomist.project.{ProjectOperation, ProjectOperationArguments}
import com.atomist.rug.InvalidRugParameterPatternException
import com.atomist.rug.kind.DefaultTypeRegistry
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.parser.DefaultIdentifierResolver
import com.atomist.rug.runtime.js.interop.SafeCommittingProxy
import com.atomist.rug.runtime.rugdsl.ContextAwareProjectOperation
import com.atomist.rug.spi.TypeRegistry
import com.atomist.source.ArtifactSource
import com.typesafe.scalalogging.LazyLogging
import jdk.nashorn.api.scripting.ScriptObjectMirror

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

  private val projectType = typeRegistry.findByName("project")
    .getOrElse(throw new TypeNotPresentException("project", null))

  readTagsFromMetadata.foreach(t => addTag(t))

  readParametersFromMetadata.foreach(p => addParameter(p))

  protected var _context: Seq[ProjectOperation] = Nil

  override def setContext(ctx: Seq[ProjectOperation]): Unit =
    _context = ctx

  protected def context: Seq[ProjectOperation] = _context

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
        case poa: ProjectOperationArguments => {
          acc :+ poa.parameterValues.map(p => p.getName -> p.getValue).toMap.asJava
        }
        case x => acc :+ x
      }
    )

    jsVar.callMember(member,processedArgs: _* )

  }

  protected def readTagsFromMetadata: Seq[Tag] = {
    Try {
      jsVar.getMember("tags") match {
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

        val p = Parameter(details.get("name").asInstanceOf[String], details.get("pattern").asInstanceOf[String])
        p.setDisplayName(details.get("displayName").asInstanceOf[String])
        p.setMaxLength(details.get("maxLength").asInstanceOf[Int])
        p.setMinLength(details.get("minLength").asInstanceOf[Int])
        p.setDefaultRef(details.get("defaultRef").asInstanceOf[String])
        p.setDisplayable(details.get("displayable").asInstanceOf[Boolean])
        p.setRequired(details.get("required").asInstanceOf[Boolean])
        details.get("default") match {
          case x: String => p.setDefaultValue(x.toString)
          case _ =>
        }

        p.setValidInputDescription(details.get("validInputDescription").asInstanceOf[String])
        p.describedAs(details.get("description").asInstanceOf[String])
        details.get("pattern").asInstanceOf[String] match {
          case s: String if s.startsWith("@") => DefaultIdentifierResolver.resolve(s.substring(1)) match {
            case Left(sourceOfValidIdentifiers) =>
              throw new InvalidRugParameterPatternException(s"Unable to recognized predefined validation pattern: $s")
            case Right(pat) => p.setPattern(pat)
          }
          case s: String if !s.startsWith("^") || !s.endsWith("$") => throw new InvalidRugParameterPatternException(s"Parameter $name does not contain anchors: $s")
          case s: String => p.setPattern(s)
          case _ =>
        }
        // p.setAllowedValues()
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
