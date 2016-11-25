package com.atomist.rug.runtime

import javax.script.ScriptContext

import com.atomist.model.content.text.{PathExpressionEngine, TreeNode}
import com.atomist.param.{Parameter, ParameterValue, Tag}
import com.atomist.project.ProjectOperationArguments
import com.atomist.project.archive.DefaultAtomistConfig
import com.atomist.project.common.support.ProjectOperationParameterSupport
import com.atomist.project.edit._
import com.atomist.project.edit.common.ProjectEditorSupport
import com.atomist.rug.RugRuntimeException
import com.atomist.rug.compiler.typescript.TypeScriptCompiler
import com.atomist.rug.kind.core.ProjectMutableView
import com.atomist.rug.spi.InstantEditorFailureException
import com.atomist.source.{ArtifactSource, FileArtifact}
import com.atomist.util.Timing._
import com.typesafe.scalalogging.LazyLogging
import jdk.nashorn.api.scripting.{AbstractJSObject, JSObject, ScriptObjectMirror}

import scala.collection.JavaConverters._
import scala.util.Try


case class Match(root: TreeNode, matches: _root_.java.util.List[TreeNode]) {
}

/**
  * JavaScript-friendly facade to PathExpressionEngine
  */
class PathExpressionExposer {

  val pee = new PathExpressionEngine

  def evaluate(tn: TreeNode, pe: Object): Match = {
    println("Foo bar")
    pe match {
      case som: ScriptObjectMirror =>
        val expr: String = som.get("expression").asInstanceOf[String]
        pee.evaluate(tn, expr) match {
          case Right(nodes) =>
            val m = Match(tn, nodes.asJava)
            m
        }
    }
  }
}

trait Registry {

  def registry: Map[String, Object]

}

object DefaultRegistry extends Registry {

  override val registry = Map(
    "PathExpressionEngine" -> new PathExpressionExposer
  )
}

/**
  * Find and instantiate JavaScript editors in an archive
  */
object JavaScriptInvokingRugEditor {

  val allJsFiles: FileArtifact => Boolean = f => f.name.endsWith(".js")
  val allTsFiles: FileArtifact => Boolean = f => f.name.endsWith(".ts")

  def fromTypeScriptArchive(rugAs: ArtifactSource, registry: Registry = DefaultRegistry): Seq[JavaScriptInvokingRugEditor] = {
    val jsc = new JavaScriptContext

    // First, compile any TypeScript files
    val tsc = //ServiceLoaderCompilerRegistry.findAll(rugAs).reduce((a, b) => a compose b)
    // ServiceLoaderCompilerRegistry.findAll(rugAs).headOption.getOrElse(???)
    new TypeScriptCompiler

    val compiled = tsc.compile(rugAs)
    val js = compiled.allFiles.filter(allJsFiles)
      .map(f => {
        //println(f.path)
        //println(f.content)
        f
      }).foreach(f => {
      jsc.eval(f)
    })

    instantiateOperationsToMakeMetadataAccessible(jsc, registry)

    val eds = editorsFromVars(rugAs, jsc)
    eds
  }

  /**
    * Convenience function to extract some metadata
    *
    * @param jsc
    * @param mirror
    * @param key
    * @return
    */
  private def get_meta(jsc: JavaScriptContext, mirror: ScriptObjectMirror, key: String): Object = {
    Try {
      jsc.engine.invokeFunction("get_metadata", mirror, key)
    }.getOrElse("not the droids you're looking for")
  }

  private def instantiateOperationsToMakeMetadataAccessible(jsc: JavaScriptContext, registry: Registry): Unit = {
    jsc.vars.filter(v => "editor".equals(get_meta(jsc, v.scriptObjectMirror, "rug-type"))).foreach(editor => {

      val args = get_meta(jsc, editor.scriptObjectMirror, "injects") match {
        case i: ScriptObjectMirror => {
          val sorted = i.asInstanceOf[ScriptObjectMirror].values().asScala.toSeq.sortBy(arg => arg.asInstanceOf[ScriptObjectMirror].get("parameterIndex").asInstanceOf[Int])
          val arg = sorted.map { arg =>
            registry.registry.get(arg.asInstanceOf[ScriptObjectMirror].get("typeToInject").asInstanceOf[String])
          }
          arg
        }.toList.map { s => s.get } //TODO how did we end up with Options here?
        case _ => Seq()
      }

      val eObj = jsc.engine.eval(editor.key).asInstanceOf[JSObject]
      val newEditor = eObj.newObject(args: _*)
      //lower case type name for instance!
      jsc.engine.put(editor.key.toLowerCase, newEditor)
    })

  }

  private def editorsFromVars(rugAs: ArtifactSource, jsc: JavaScriptContext): Seq[JavaScriptInvokingRugEditor] = {

    jsc.vars.map(v => (v, Try {
      jsc.engine.invokeFunction("get_metadata", v.scriptObjectMirror, "rug-type").asInstanceOf[String]
    }.toOption)) collect {
      case (v, Some(rugType)) if "editor".equals(rugType) =>
        new JavaScriptInvokingRugEditor(jsc, v.key, v.scriptObjectMirror, rugAs)
    }
  }
}


abstract class JavaScriptInvokingRugOperation(
                                               jsc: JavaScriptContext,
                                               className: String,
                                               jsVar: ScriptObjectMirror,
                                               rugAs: ArtifactSource
                                             )
  extends ProjectOperationParameterSupport
    with LazyLogging {

  override val name: String =
    if (className.endsWith("Editor")) className.dropRight("Editor".length)
    else className

  readTagsFromMetadata.foreach(t => addTag(t))

  readParametersFromMetadata.foreach(p => addParameter(p))

  override def description: String = jsc.engine.invokeFunction("get_metadata", jsVar, "editor-description") match {
    case s: String => s
    case _ => name
  }

  protected def readTagsFromMetadata: Seq[Tag] = {
    Try {
      jsc.engine.invokeFunction("get_metadata", jsVar, "tags") match {
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
    Try {
      val vars = jsc.engine.getContext.getBindings(ScriptContext.ENGINE_SCOPE)
      val pclass = jsc.engine.invokeFunction("get_metadata", jsVar, "parameter-class").asInstanceOf[String]
      jsc.engine.invokeFunction("get_metadata", vars.get(pclass).asInstanceOf[ScriptObjectMirror], "params") match {
        case som: ScriptObjectMirror =>
          val values = som.asScala collect {
            case (name: String, details: AnyRef) => {
              //TODO - can we do some fancy data binding here? map keys match setters (mostly)
              val p = Parameter(name, details.asInstanceOf[ScriptObjectMirror].get("pattern").asInstanceOf[String])
              p.setDisplayName(details.asInstanceOf[ScriptObjectMirror].get("displayName").asInstanceOf[String])
              p.setMaxLength(details.asInstanceOf[ScriptObjectMirror].get("maxLength").asInstanceOf[Int])
              p.setMinLength(details.asInstanceOf[ScriptObjectMirror].get("minLength").asInstanceOf[Int])
              p.setDefaultRef(details.asInstanceOf[ScriptObjectMirror].get("defaultRef").asInstanceOf[String])
              p.setDisplayable(details.asInstanceOf[ScriptObjectMirror].get("displayable").asInstanceOf[Boolean])
              p.setRequired(details.asInstanceOf[ScriptObjectMirror].get("required").asInstanceOf[Boolean])
              p.setDefaultValue(details.asInstanceOf[ScriptObjectMirror].get("defaultValue").asInstanceOf[String])
              p.setValidInputDescription(details.asInstanceOf[ScriptObjectMirror].get("validInputDescription").asInstanceOf[String])
              p.describedAs(details.asInstanceOf[ScriptObjectMirror].get("description").asInstanceOf[String])
              //TODO it's unclear what allowedValues is for given an AllowedValue is just a name/display_name mapping
              //p.setAllowedValues()
              p
            }
          }
          values.toSeq
        case _ => Nil
      }
    }.getOrElse(Nil)
  }

}

/**
  * ProjectEditor implementation that invokes a JavaScript function. This will probably be the result of
  * TypeScript compilation, but need not be. Attempts to source metadata from annotations.
  */
class JavaScriptInvokingRugEditor private(
                                           jsc: JavaScriptContext,
                                           className: String,
                                           jsVar: ScriptObjectMirror,
                                           rugAs: ArtifactSource
                                         )
  extends JavaScriptInvokingRugOperation(jsc, className, jsVar, rugAs)
    with ProjectEditorSupport {

  override def impacts: Set[Impact] = Impacts.UnknownImpacts

  override def applicability(as: ArtifactSource): Applicability = Applicability.OK

  override protected def modifyInternal(
                                         targetProject: ArtifactSource,
                                         poa: ProjectOperationArguments): ModificationAttempt = {
    val tr = time {
      val pmv = new ProjectMutableView(rugAs, targetProject, atomistConfig = DefaultAtomistConfig)

      val params = new Parameters(poa)

      //  println(editMethod.entrySet())

      try {
        //important that we don't invoke edit on the prototype as otherwise all constructor effects are lost!
        val res = jsc.engine.get(className.toLowerCase).asInstanceOf[ScriptObjectMirror].callMember("edit", pmv, params)

        if (pmv.currentBackingObject == targetProject) {
          NoModificationNeeded("OK")
        }
        else {
          SuccessfulModification(pmv.currentBackingObject, impacts, "OK")
        }
      }
      catch {
        case f: InstantEditorFailureException =>
          FailedModificationAttempt(f.getMessage)
      }

    }
    logger.debug(s"$name modifyInternal took ${tr._2}ms")
    tr._1
  }

}


/**
  * Dynamic properties holder that represents the JVM counterpart of a TypeScript class that
  * doesn't exist in Java.
  */
private class Parameters(poa: ProjectOperationArguments) extends AbstractJSObject {

  override def getMember(name: String): AnyRef = {
    val resolved: ParameterValue = poa.parameterValueMap.getOrElse(
      name,
      throw new RugRuntimeException(null, s"Cannot resolve parameter [$name]"))
    //println(s"Call to getMember with [$name]")

    // The below is what you use for a function
    //    new AbstractJSObject() {
    //
    //      override def isFunction: Boolean = true
    //
    //      override def call(thiz: scala.Any, args: AnyRef*): AnyRef = {
    //        resolved
    //      }
    //    }

    // This works for a method
    resolved.getValue
    // TODO fall back to the value in the field?
  }
}

