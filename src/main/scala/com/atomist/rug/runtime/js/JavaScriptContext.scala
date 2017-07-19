package com.atomist.rug.runtime.js

import javax.script.{Bindings, ScriptEngine, SimpleBindings}

import com.atomist.param.ParameterValues
import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.rug.runtime.js.nashorn.NashornJavaScriptEngine
import com.atomist.source.{ArtifactSource, FileArtifact}

/**
  * For backwards compatibility with CLI/runner
  */
class JavaScriptContext(as: ArtifactSource,
                        config: AtomistConfig = DefaultAtomistConfig,
                        bindings: Bindings = new SimpleBindings(),
                        initializer: JavaScriptContext.EngineInitializer = NashornJavaScriptEngine.redirectConsoleToSysOut)

  extends JavaScriptEngine {

  private val engine = JavaScriptEngineContextFactory.create(as)
  /**
    * Function that can initialize a ScriptEngine before use.
    * Typically evaluates JavaScript strings or binds objects.
    */
  override def evaluate(f: FileArtifact): Unit = engine.evaluate(f)

  override def atomistConfig: AtomistConfig = engine.atomistConfig

  override def members(): Seq[JavaScriptMember] = engine.members()

  override def rugAs: ArtifactSource = engine.rugAs

  override def invokeMember(jsVar: JavaScriptObject, member: String, params: Option[ParameterValues], args: Object*): AnyRef = engine.invokeMember(jsVar, member, params, args:_*)

  override def parseJson(json: String): JavaScriptObject = engine.parseJson(json)

  override def setMember(name: String, value: AnyRef): Unit = engine.setMember(name, value)

  override def atomistContent(): ArtifactSource = engine.atomistContent()

  override def eval(script: String): AnyRef = eval(script)
}

object JavaScriptContext {
  type EngineInitializer = ScriptEngine => Unit
}
