package com.atomist.rug.runtime.js

import javax.script.ScriptContext

import com.atomist.project.ProjectOperation
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.rug.runtime.js.interop.{DefaultAtomistFacade, UserModelContext}
import com.atomist.source.{ArtifactSource, FileArtifact}
import jdk.nashorn.api.scripting.ScriptObjectMirror

/**
  * Find and instantiate JavaScript editors in a Rug archive
  */
object JavaScriptOperationFinder {

  val ExecutorType = "executor"

  val EditorType = "editor"

  val GeneratorType = "generator"

  /**
    * Used to recognise JS operations that we can call.
    * TODO - this should probably include type checking too!
    */
  val KnownSignatures = Map(
    ExecutorType -> JsRugOperationSignature(Set("execute")),
    EditorType -> JsRugOperationSignature(Set("edit")),
    GeneratorType -> JsRugOperationSignature(Set("populate")))

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

      insertRegistry(jsc,registry)
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


  //todo clean up this dispatch/signature stuff - too coupled
  private def operationsFromVars(rugAs: ArtifactSource, jsc: JavaScriptContext): Seq[JavaScriptInvokingProjectOperation] = {
    jsc.vars.map(v => (v, extractOperation(v.scriptObjectMirror))) collect {
      case (v, Some(EditorType)) =>
        new JavaScriptInvokingProjectEditor(jsc, v.scriptObjectMirror, rugAs)
      case (v, Some(GeneratorType)) =>
        new JavaScriptInvokingProjectGenerator(jsc, v.scriptObjectMirror, rugAs)
      case (v, Some(ExecutorType)) =>
        new JavaScriptInvokingExecutor(jsc, v.scriptObjectMirror, rugAs)
    }
  }

  private def extractOperation(obj: ScriptObjectMirror): Option[String] = {
    val matches = KnownSignatures.foldLeft(Seq[String]())((acc : Seq[String], kv) => {
      //does it contain all the matching functions and props?
      val fns = kv._2.functionsNames
      val props = kv._2.propertyNames
      val fnCount = fns.count (fn => {
        obj.hasMember(fn) && obj.getMember(fn).asInstanceOf[ScriptObjectMirror].isFunction
      })
      val propsCount = props.count (prop => {
        obj.hasMember(prop)//TODO make stronger check
      })
      if(fnCount == fns.size && propsCount == props.size){
        acc :+ kv._1
      }else{
        acc
      }
    })
    matches.headOption
  }

  case class JsRugOperationSignature(functionsNames: Set[String], propertyNames: Set[String] = Set("name", "description")){

  }

  /**
    * Inject a registry implementation in to the context
    * @param jsc
    */
  private def insertRegistry(jsc: JavaScriptContext, ctx: UserModelContext = DefaultAtomistFacade){
    jsc.engine.getContext().setAttribute("atomist_registry", new Registry(ctx.registry), ScriptContext.ENGINE_SCOPE)
  }
  class Registry(registry: Map[String,Object]){
    def lookup(id: String): Object ={
      registry.getOrElse(id, Nil)
    }
  }
}

