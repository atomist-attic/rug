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

  def fromTypeScriptArchive(rugAs: ArtifactSource,
                            registry: UserModelContext = DefaultUserModelContext): Seq[ProjectOperation] = {
    val jsc = new JavaScriptContext

    // First, compile any TypeScript files
    val tsc = //ServiceLoaderCompilerRegistry.findAll(rugAs).reduce((a, b) => a compose b)
    // ServiceLoaderCompilerRegistry.findAll(rugAs).headOption.getOrElse(???)
    new TypeScriptCompiler

    val compiled = tsc.compile(rugAs)
    val js = compiled.allFiles.filter(allJsFiles)
      .map(f => {
        //println(f.path)
        println(f.content)
        f
      }).foreach(f => {
      jsc.eval(f)
    })

    instantiateOperationsToMakeMetadataAccessible(jsc, registry)

    val eds = operationsFromVars(rugAs, jsc)
    eds
  }

  private def instantiateOperationsToMakeMetadataAccessible(jsc: JavaScriptContext, registry: UserModelContext): Unit = {
    for {
      v <- jsc.vars
      rugType <- v.getMetaString("rug-type")
      if Set("editor", "generator").contains(rugType.toString)
    } {
      val args = jsc.getMeta(v.scriptObjectMirror, "injects") match {
        case Some(i: ScriptObjectMirror) => {
          val sorted = i.asInstanceOf[ScriptObjectMirror].values().asScala.toSeq.sortBy(arg => arg.asInstanceOf[ScriptObjectMirror].get("parameterIndex").asInstanceOf[Int])
          sorted.flatMap { arg =>
            registry.registry.get(arg.asInstanceOf[ScriptObjectMirror].get("typeToInject").asInstanceOf[String])
          }
        }
        case _ => Seq()
      }

      val eObj = jsc.engine.eval(v.key).asInstanceOf[JSObject]
      val newEditor = eObj.newObject(args: _*)
      //lower case type name for instance!
      jsc.engine.put(v.key.toLowerCase, newEditor)
    }
  }

  private def operationsFromVars(rugAs: ArtifactSource, jsc: JavaScriptContext): Seq[JavaScriptInvokingProjectOperation] = {
    jsc.vars.map(v => (v, v.getMetaString("rug-type"))) collect {
      case (v, Some("editor")) =>
        new JavaScriptInvokingProjectEditor(jsc, v.key, v.scriptObjectMirror, rugAs)
      case (v, Some("generator")) =>
        new JavaScriptInvokingProjectGenerator(jsc, v.key, v.scriptObjectMirror, rugAs)
    }
  }
}
