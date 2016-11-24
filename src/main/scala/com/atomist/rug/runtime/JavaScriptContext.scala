package com.atomist.rug.runtime

import java.nio.charset.StandardCharsets
import javax.script.{Invocable, ScriptContext, ScriptEngine, ScriptEngineManager}

import com.atomist.source.FileArtifact
import com.typesafe.scalalogging.LazyLogging
import jdk.nashorn.api.scripting.ScriptObjectMirror
import org.apache.commons.io.IOUtils

import scala.collection.JavaConverters._

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

  //so we can print stuff out from TS
  val consolejs = """console = {
                        log: print,
                        warn: print,
                        error: print
                    };"""

  engine.eval(consolejs)

  // Avoid the problem with export
  engine.eval("exports = {}")

  val npmJs = IOUtils.toString(getClass.getResourceAsStream("/utils/jvm-npm.js"), StandardCharsets.UTF_8)
  engine.eval(npmJs)

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

  case class Var(key: String, scriptObjectMirror: ScriptObjectMirror)

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

}
