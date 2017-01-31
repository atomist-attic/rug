package com.atomist.rug.runtime.js

import java.util.regex.Pattern
import javax.script._

import com.atomist.project.archive.{AtomistConfig, DefaultAtomistConfig}
import com.atomist.rug.RugJavaScriptException
import com.atomist.source.ArtifactSource
import com.coveo.nashorn_modules.{AbstractFolder, Folder, Require}
import com.typesafe.scalalogging.LazyLogging
import jdk.nashorn.api.scripting.{NashornScriptEngine, NashornScriptEngineFactory, ScriptObjectMirror}

import scala.collection.JavaConverters._

/**
  * Context superclass for evaluating JavaScript.
  * Creates a Nashorn ScriptEngineManager and can evaluate files and JavaScript fragments,
  * exposing the known vars in a typesafe way so we partly avoid the horrific detyped
  * Nashorn API.
  *
  * One of these per rug please, or else they may stomp on one-another
  */
class JavaScriptContext(rugAs: ArtifactSource,
                        atomistConfig: AtomistConfig = DefaultAtomistConfig,
                        bindings: Bindings = new SimpleBindings()) extends LazyLogging {

  val engine: NashornScriptEngine =
    new NashornScriptEngineFactory()
      .getScriptEngine("--optimistic-types", "--language=es6", "--no-java")
      .asInstanceOf[NashornScriptEngine]

  engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE)

  private val consoleJs =
    """
      |console = {
      |   log: print,
      |   warn: print,
      |   error: print
      |};
    """.stripMargin

  //so we can print stuff out from TS
  engine.eval(consoleJs)

  try {
    Require.enable(engine, new ArtifactSourceBasedFolder(rugAs))
  } catch {
    case e: Exception =>
      throw new RuntimeException("Unable to set up ArtifactSource based module loader", e)
  }

  private val filtered = atomistConfig.atomistContent(rugAs)
    .filter(d => true,
      f => atomistConfig.isJsSource(f))

  //require all the atomist stuff
  for (f <- filtered.allFiles) {
    val varName = f.path.dropRight(3).replaceAll("/", "_").replaceAll("\\.", "\\$")
    try {
      engine.eval(s"exports.$varName = require('./${f.path.dropRight(3)}');") //because otherwise the loader doesn't know about the paths and can't resolve relative modules
    } catch {
      case x: ScriptException => throw new RugJavaScriptException(s"Error during eval of: ${f.path}", x)
      case x: RuntimeException => x.getCause match {
        case c: ScriptException => throw new RugJavaScriptException(s"Error during eval of: ${f.path}", c)
        case c => throw x
      }
    }
  }


  /**
    * Information about a JavaScript var exposed in the project scripts
    *
    * @param key                name of the var
    * @param scriptObjectMirror interface for working with Var
    */
  case class Var(key: String, scriptObjectMirror: ScriptObjectMirror) {}

  /**
    * Return all the vars known to the engine that expose ScriptObjectMirror objects, with the key
    *
    * @return ScriptObjectMirror objects for all vars known to the engine
    */
  def vars: Seq[Var] = {
    val res = engine.getContext.getBindings(ScriptContext.ENGINE_SCOPE)
      .get("exports").asInstanceOf[ScriptObjectMirror]
      .asScala
      .foldLeft(Seq[Var]())((acc: Seq[Var], kv) => {
        acc ++ extractVars(kv._2.asInstanceOf[ScriptObjectMirror])
      })
    res
  }

  private def extractVars(obj: ScriptObjectMirror): Seq[Var] = {
    obj.entrySet().asScala.flatMap(e => {
      (e.getKey, e.getValue) match {
        case (k, som: ScriptObjectMirror) => Some(Var(k, som))
        case _ => None
      }
    }).toSeq
  }

  private class ArtifactSourceBasedFolder private(val artifacts: ArtifactSource, val parent: Folder, val path: String) extends AbstractFolder(parent, path) {

    private val commentPattern: Pattern = Pattern.compile("^//.*$", Pattern.MULTILINE)

    def this(artifacts: ArtifactSource, rootPath: String = "") {
      this(artifacts.underPath(rootPath), null, "")
    }

    def getFile(s: String): String = {
      val file = artifacts.findFile(getPath + s)
      if (file.isEmpty) return null
      //remove these source-map comments because they seem to be breaking nashorn :/
      commentPattern.matcher(file.get.content).replaceAll("")
    }

    def getFolder(s: String): Folder = {
      val dir = artifacts.findDirectory(getPath + s)
      if (dir.isEmpty) return null
      new ArtifactSourceBasedFolder(artifacts, this, getPath + s + "/")
    }
  }

}
