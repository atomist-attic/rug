package com.atomist.rug.runtime.js

import com.atomist.project.ProjectOperation
import com.atomist.project.archive.AtomistConfig
import com.atomist.rug.compiler.typescript.TypeScriptCompiler
import com.atomist.rug.kind.core.{FileArtifactBackedMutableView, ProjectMutableView}
import com.atomist.rug.runtime._
import com.atomist.rug.runtime.js.interop.{DefaultUserModelContext, UserModelContext}
import com.atomist.source.{ArtifactSource, FileArtifact}
import com.atomist.util.lang.TypescriptArray
import jdk.nashorn.api.scripting.{JSObject, ScriptObjectMirror}

import scala.collection.JavaConverters._

/**
  * Find and instantiate JavaScript editors in an archive
  */
object JavaScriptOperationFinder {

  val ExecutorType = "executor"

  val EditorType = "editor"

  val GeneratorType = "generator"

  /**
    * Rug types expressed in decorators that we know about
    */
  val KnownOperationTypes = Set(ExecutorType, EditorType, GeneratorType)

  // All known JS and TS files not in user_modules
  val allJsFiles: FileArtifact => Boolean = f => f.name.endsWith(".js")
  val allTsFiles: FileArtifact => Boolean = f => f.name.endsWith(".ts")

  def fromTypeScriptArchive(rugAs: ArtifactSource,
                            registry: UserModelContext = DefaultUserModelContext): Seq[ProjectOperation] = {
    val jsc = new JavaScriptContext

    // First, compile any TypeScript files
    val tsc = new TypeScriptCompiler
    val compiled = tsc.compile(rugAs)
    compiled.allFiles.filter(f => !f.path.startsWith(".atomist/node_modules/"))
      .filter(allJsFiles)
      .foreach(f => {
        //println(f.content)
        jsc.eval(f)
      })

    instantiateOperationsToMakeMetadataAccessible(jsc, registry)

    val operations = operationsFromVars(rugAs, jsc)
    jsc.shutdown()
    operations
  }

  private def instantiateOperationsToMakeMetadataAccessible(jsc: JavaScriptContext, registry: UserModelContext): Unit = {
    for {
      v <- jsc.vars
      rugType <- v.getMetaString("rug-type")
      if KnownOperationTypes.contains(rugType.toString)
    } {
      val args = jsc.getMeta(v.scriptObjectMirror, "injects") match {
        case Some(i: ScriptObjectMirror) =>
          val sorted = i.asInstanceOf[ScriptObjectMirror].values().asScala.toSeq.sortBy(arg => arg.asInstanceOf[ScriptObjectMirror].get("parameterIndex").asInstanceOf[Int])
          sorted.flatMap { arg =>
            registry.registry.get(arg.asInstanceOf[ScriptObjectMirror].get("typeToInject").asInstanceOf[String])
          }
        case _ => Seq()
      }

      val eObj = jsc.engine.eval(v.key).asInstanceOf[JSObject]
      val newEditor = eObj.newObject(args: _*)
      // Lower case type name for instance!
      jsc.engine.put(v.key.toLowerCase, newEditor)
    }
  }

  private def operationsFromVars(rugAs: ArtifactSource, jsc: JavaScriptContext): Seq[JavaScriptInvokingProjectOperation] = {
    jsc.vars.map(v => (v, v.getMetaString("rug-type"))) collect {
      case (v, Some(EditorType)) =>
        new JavaScriptInvokingProjectEditor(jsc, v.key, v.scriptObjectMirror, rugAs)
      case (v, Some(GeneratorType)) =>
        new JavaScriptInvokingProjectGenerator(jsc, v.key, v.scriptObjectMirror, rugAs)
      case (v, Some(ExecutorType)) =>
        new JavaScriptInvokingExecutor(jsc, v.key, v.scriptObjectMirror, rugAs)
    }
  }
}

class TypescriptArrayDecoratingProjectMutableView(rugAs: ArtifactSource,
                                                  originalBackingObject: ArtifactSource,
                                                  atomistConfig: AtomistConfig,
                                                  context: Seq[ProjectOperation] = Nil)
  extends ProjectMutableView(rugAs, originalBackingObject , atomistConfig, context) {

  override def files: java.util.List[FileArtifactBackedMutableView] = {
    new TypescriptArray[FileArtifactBackedMutableView](super.files)
  }
}
