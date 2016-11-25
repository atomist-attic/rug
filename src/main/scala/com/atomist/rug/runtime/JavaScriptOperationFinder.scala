package com.atomist.rug.runtime

import com.atomist.project.ProjectOperation
import com.atomist.rug.compiler.typescript.TypeScriptCompiler
import com.atomist.source.{ArtifactSource, FileArtifact}
import jdk.nashorn.api.scripting.{JSObject, ScriptObjectMirror}

import scala.util.Try
import scala.collection.JavaConverters._

/**
  * Find and instantiate JavaScript editors in an archive
  */
object JavaScriptOperationFinder {

  val allJsFiles: FileArtifact => Boolean = f => f.name.endsWith(".js")
  val allTsFiles: FileArtifact => Boolean = f => f.name.endsWith(".ts")

  def fromTypeScriptArchive(rugAs: ArtifactSource, registry: Registry = DefaultRegistry): Seq[ProjectOperation] = {
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
