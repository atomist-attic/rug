package com.atomist.rug.runtime.js

import com.atomist.project.ProjectOperation
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.rug.compiler.typescript.TypeScriptCompiler
import com.atomist.rug.runtime.js.interop.{DefaultAtomistFacade, UserModelContext}
import com.atomist.source.{ArtifactSource, FileArtifact, SimpleFileBasedArtifactSource}
import jdk.nashorn.api.scripting.{JSObject, ScriptObjectMirror}

import scala.collection.JavaConverters._

/**
  * Find and instantiate JavaScript editors in a Rug archive
  */
object JavaScriptOperationFinder {

  val ExecutorType = "executor"

  val EditorType = "editor"

  val GeneratorType = "generator"

  /**
    * Rug types expressed in decorators that we know about
    */
  val KnownOperationTypes = Set(ExecutorType, EditorType, GeneratorType)

  val jsFile: FileArtifact => Boolean = f => f.name.endsWith(".js")
  val tsFile: FileArtifact => Boolean = f => f.name.endsWith(".ts")

  private def excludedTypeScriptPath(atomistConfig: AtomistConfig) =
    s"${atomistConfig.atomistRoot}/node_modules/"

  /**
    * Find and instantiate project operations in the given Rug archive
    *
    * @param rugAs          archive to look into
    * @param registry       registry to allow operations to be looked up.
    *                       Injected into operations that ask for it
    * @param shouldSuppress function allowing us to specify which JS
    *                       files we should suppress. Suppresses none by default.
    * @return a sequence of instantiated operations backed by JavaScript compiled
    *         from TypeScript
    */
  def fromTypeScriptArchive(rugAs: ArtifactSource,
                            registry: UserModelContext = DefaultAtomistFacade,
                            atomistConfig: AtomistConfig = DefaultAtomistConfig,
                            shouldSuppress: FileArtifact => Boolean = f => false): Seq[ProjectOperation] = {
    val jsc = new JavaScriptContext

    try {
      val compiled = filterAndCompile(rugAs, atomistConfig, shouldSuppress, jsc)

      for (f <- compiled.allFiles) {
        jsc.eval(f)
      }

      instantiateOperationsToMakeMetadataAccessible(jsc, registry)
      val operations = operationsFromVars(rugAs, jsc)
      operations
    }
    finally {
      jsc.shutdown()
    }
  }

  /**
    * Filter and compile all JavaScript files under Atomist folder in the given archive
    *
    * @param rugAs          Archive to look in
    * @param atomistConfig  configuration
    * @param shouldSuppress function allowing us to specify which JS
    *                       files we should suppress. Suppresses none by default.
    * @return
    */
  def filterAndCompile(rugAs: ArtifactSource,
                       atomistConfig: AtomistConfig,
                       shouldSuppress: FileArtifact => Boolean = f => false,
                       jsc: JavaScriptContext): ArtifactSource = {
    val tsc = jsc.typeScriptContext.compiler()
    val excludePrefix = excludedTypeScriptPath(atomistConfig)
    val filtered = atomistConfig.atomistContent(rugAs)
      .filter(d => true,
        f => (jsFile(f) || tsFile(f))
          && f.path.startsWith(atomistConfig.atomistRoot) && !f.path.startsWith(excludePrefix) && !shouldSuppress(f))

    tsc.compile(filtered)
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
      val newOperation = eObj.newObject(args: _*)
      // Lower case type name for instance!
      jsc.engine.put(v.key.toLowerCase, newOperation)
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

