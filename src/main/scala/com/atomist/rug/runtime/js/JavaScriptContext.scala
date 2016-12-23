package com.atomist.rug.runtime.js

import javax.script.ScriptContext

import com.atomist.source.{ArtifactSource, FileArtifact}
import com.coveo.nashorn_modules.{AbstractFolder, Folder, Require}
import com.typesafe.scalalogging.LazyLogging
import jdk.nashorn.api.scripting.{ClassFilter, NashornScriptEngine, NashornScriptEngineFactory, ScriptObjectMirror}

import scala.collection.JavaConverters._

/**
  * Context superclass for evaluating JavaScript.
  * Creates a Nashorn ScriptEngineManager and can evaluate files and JavaScript fragments,
  * exposing the known vars in a typesafe way so we partly avoid the horrific detyped
  * Nashorn API.
  */
class JavaScriptContext(rugAs: ArtifactSource, allowedClasses: Set[String] = Set.empty[String]) extends LazyLogging {

  private val commonOptions = Array("--optimistic-types", "--language=es6")

  /**
    * At the time of writing, allowedClasses were only used for test.
    *
    * If you do need to expose some classes to JS, then make sure you configure to use a locked down classloader and security manager
    */
  val engine: NashornScriptEngine =
      new NashornScriptEngineFactory().getScriptEngine(
        if(allowedClasses.isEmpty) commonOptions :+ "--no-java" else commonOptions,
        if(allowedClasses.isEmpty) null else Thread.currentThread().getContextClassLoader,//TODO - do we need our own loader here?
        new ClassFilter {
          override def exposeToScripts(s: String): Boolean = {
            allowedClasses.contains(s)
          }
        }
      ).asInstanceOf[NashornScriptEngine]

  configureEngine(engine)

  /**
    * Evaluate the contents of the file or do nothing if it's not JavaScript
    * @param f file to evaluate
    */
  def eval(f: FileArtifact): Unit = {
    if (f.name.endsWith(".js"))
      engine.eval(f.content)
  }

  /**
    * Information about a JavaScript var exposed in the project scripts
    * @param key name of the var
    * @param scriptObjectMirror interface for working with Var
    */
  case class Var(key: String, scriptObjectMirror: ScriptObjectMirror) {
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



  private def configureEngine(scriptEngine: NashornScriptEngine): Unit = {
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
    try
      Require.enable(engine, new ArtifactSourceBasedFolder(rugAs))
    catch {
      case e: Exception => {
        throw new RuntimeException("Unable to set up ArtifactSource based module loader", e)
      }
    }
  }

  private class ArtifactSourceBasedFolder private(var artifacts: ArtifactSource, val parent: Folder, val path: String) extends AbstractFolder(parent, path) {
    def this(artifacts: ArtifactSource) {
      this(artifacts.underPath(".atomist"), null, "")
    }

    def getFile(s: String): String = {
      val file = artifacts.findFile(s)
      if (file.isEmpty) return null
      file.get.content
    }

    def getFolder(s: String): Folder = {
      val dir = artifacts.findDirectory(s)
      if (dir.isEmpty) return null
      new ArtifactSourceBasedFolder(artifacts.underPath(s), this, getPath + s + "/")
    }
  }
}
