package com.atomist.rug.runtime

import javax.script.{Invocable, ScriptContext, ScriptEngine, ScriptEngineManager}

import com.atomist.rug.compiler.typescript.TypeScriptCompilerContext
import com.atomist.source.FileArtifact
import com.typesafe.scalalogging.LazyLogging
import jdk.nashorn.api.scripting.ScriptObjectMirror

import scala.collection.JavaConverters._
import scala.util.Try

/**
  * Context superclass for evaluating JavaScript.
  * Creates a Nashorn ScriptEngineManager and can evaluate files and JavaScript fragments,
  * exposing the known vars in a typesafe way so we partly avoid the horrific detyped
  * Nashorn API.
  */
class JavaScriptContext extends LazyLogging {

  val engine: ScriptEngine with Invocable =
    new ScriptEngineManager(null).getEngineByName("nashorn") match {
      case is: ScriptEngine with Invocable => is
    }

  val typeScriptContext = new TypeScriptCompilerContext

  configureEngine(engine)

  /**
    * Evaluate the given JS fragment
    * @param js JavaScript
    */
  def eval(js: String): Unit = {
    engine.eval(js)
  }

  /**
    * Evaluate the contents of the file or do nothing if it's not JavaScript
    * @param f file to evaluate
    */
  def eval(f: FileArtifact): Unit = {
    if (f.name.endsWith(".js"))
      eval(f.content)
  }

  /**
    * Information about a JavaScript var exposed in the project scripts
    * @param key name of the var
    * @param scriptObjectMirror interface for working with Var
    */
  case class Var(key: String, scriptObjectMirror: ScriptObjectMirror) {

    def getMetaString(key: String): Option[String] =
      JavaScriptContext.this.getMeta(scriptObjectMirror, key) match {
        case Some(s: String) => Some(s)
        case _ => None
      }

  }

  /**
    * Convenience function to extract some metadata
    *
    * @param mirror
    * @param key
    * @return
    */
  def getMeta(mirror: ScriptObjectMirror, key: String): Option[Any] = {
    Try {
      engine.invokeFunction("get_metadata", mirror, key)
    }.toOption
  }

  /**
    * Return all the vars known to the engine that expose ScriptObjectMirror objects, with the key
    * @return ScriptObjectMirror objects for all vars known to the engine
    */
  def vars: Seq[Var] =
    engine.getContext.getBindings(ScriptContext.ENGINE_SCOPE).entrySet().asScala.flatMap(e => {
      (e.getKey, e.getValue) match {
        case (k, som: ScriptObjectMirror) => Some(Var(k, som))
        case _ => None
      }
    }).toSeq

  /**
    * Shutdown the compiler context after successful extraction of operations
    */
  def shutdown() = {
    typeScriptContext.shutdown()
  }

  private def configureEngine(scriptEngine: ScriptEngine): Unit = {
    //so we can print stuff out from TS
    val consoleJs =
    """
      |console = {
      |   log: print,
      |   warn: print,
      |   error: print
      |};
    """.stripMargin
    scriptEngine.eval(consoleJs)

    typeScriptContext.init(scriptEngine)
  }

}
